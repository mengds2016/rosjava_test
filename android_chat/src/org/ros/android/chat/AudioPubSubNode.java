package org.ros.android.chat;

import java.nio.ByteOrder;

import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.NodeMain;
import org.ros.node.topic.Publisher;
import org.ros.node.topic.Subscriber;

import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.media.AudioFormat;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;

import org.ros.node.*;

public class AudioPubSubNode implements NodeMain {

	private String package_name;

	private Publisher<audio_common_msgs.AudioData> audio_pub;
	private Subscriber<audio_common_msgs.AudioData> audio_sub;
	private audio_common_msgs.AudioData audio_msg;
	final static int SAMPLING_RATE = 8000; // 8000,11025,16000,22050,44100
	private AudioRecord audioRec = null;
	private AudioTrack audioTra = null;
	private int bufSize;
	private Thread recordThre;
	private byte[] tmp_buffer;
	private int size_buf = 0;

	@Override
	public GraphName getDefaultNodeName() {
		return GraphName.of(this.package_name+"/audio");
	}

	public AudioPubSubNode(String package_name) {
		this.package_name = package_name;
	}

	public AudioRecord findAudioRecord() {
		for (short audioFormat : new short[] { AudioFormat.ENCODING_PCM_8BIT,
				AudioFormat.ENCODING_PCM_16BIT }) {
			for (short channelConfig : new short[] {
					AudioFormat.CHANNEL_IN_MONO, AudioFormat.CHANNEL_IN_STEREO }) {
				for (int rate : new int[] { 8000, 11025, 22050, 44100 }) {
	                try {
	                	System.out.println("[findAudioRecord] try " + audioFormat + "/" + channelConfig + "/" + rate);
	                    this.bufSize = AudioRecord.getMinBufferSize(rate, channelConfig, AudioFormat.ENCODING_PCM_16BIT);

	                    if (this.bufSize != AudioRecord.ERROR_BAD_VALUE) {
	                    	AudioRecord recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, rate, channelConfig, audioFormat, this.bufSize);
	                        if (recorder.getState() == AudioRecord.STATE_INITIALIZED)
	                            return recorder;
	                    }
	                } catch (Exception e) {
	                	e.printStackTrace();
	                }
	            }
	        }
	    }
	    return null;
	}
	
	@Override
	public void onStart(final ConnectedNode connectedNode) {
		this.audio_sub = connectedNode.newSubscriber(this.package_name
				+ "/audio/in", "audio_common_msgs/AudioData");
		this.audio_sub
				.addMessageListener(new MessageListener<audio_common_msgs.AudioData>() {
					@Override
					public void onNewMessage(
							final audio_common_msgs.AudioData message) {
						ChannelBuffer heapBuffer = message.getData();
						int size = heapBuffer.readableBytes();
						if (AudioPubSubNode.this.tmp_buffer == null
								|| AudioPubSubNode.this.tmp_buffer.length < size) {
							AudioPubSubNode.this.tmp_buffer = new byte[size];
						}
						heapBuffer.readBytes(
								AudioPubSubNode.this.tmp_buffer, 0,
								size);
						if (AudioPubSubNode.this.audioTra != null) {
							System.out.println(" audio receive " + size);
							AudioPubSubNode.this.audioTra.write(
									AudioPubSubNode.this.tmp_buffer, 0, size);
							AudioPubSubNode.this.size_buf += size;
							//if (AudioPubSubNode.this.size_buf > AudioPubSubNode.this.bufSize*10){
								AudioPubSubNode.this.audioTra.play();
								AudioPubSubNode.this.size_buf = 0;
							//}
						}
					}
				});

		this.bufSize = (int)(SAMPLING_RATE * 0.5 * 2);
//		this.bufSize = AudioRecord.getMinBufferSize(SAMPLING_RATE,
//				AudioFormat.CHANNEL_CONFIGURATION_MONO,
//				AudioFormat.ENCODING_PCM_16BIT);
		this.audio_pub = connectedNode.newPublisher(this.package_name
				+ "/audio/out", "audio_common_msgs/AudioData");

		this.audioRec = new AudioRecord(MediaRecorder.AudioSource.MIC,
				SAMPLING_RATE, AudioFormat.CHANNEL_CONFIGURATION_MONO,
				AudioFormat.ENCODING_PCM_16BIT, this.bufSize);
		// this.audioRec = this.findAudioRecord();

		this.audioTra = new AudioTrack(AudioManager.STREAM_MUSIC,
				SAMPLING_RATE, AudioFormat.CHANNEL_CONFIGURATION_MONO,
				AudioFormat.ENCODING_PCM_16BIT, this.bufSize ,
				AudioTrack.MODE_STREAM);

		this.audioRec.startRecording();
		(this.recordThre = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					byte buf[] = new byte[AudioPubSubNode.this.bufSize];
					AudioPubSubNode.this.audio_msg = AudioPubSubNode.this.audio_pub
							.newMessage();
					while (AudioPubSubNode.this.recordThre != null
							&& AudioPubSubNode.this.audioRec != null) {
						int returnSize = AudioPubSubNode.this.audioRec.read(
								buf, 0, buf.length);
						ChannelBuffer heapBuffer = ChannelBuffers.buffer(
								ByteOrder.LITTLE_ENDIAN, returnSize);
						heapBuffer.writeBytes(buf, 0, returnSize);
						AudioPubSubNode.this.audio_msg.setData(heapBuffer);
						AudioPubSubNode.this.audio_pub
								.publish(AudioPubSubNode.this.audio_msg);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		})).start();

	}

	public void onDestroy(){
		if (this.audioRec != null) {
			this.audioRec.stop();
			this.audioRec.release();
			this.audioRec = null;
		}
		if ( this.recordThre != null ){
			this.recordThre = null;
		}
		if (this.audioTra != null) {
			this.audioTra.stop();
			this.audioTra.release();
			this.audioTra = null;
		}
	}
	
	@Override
	public void finalize() throws Throwable {
		onDestroy();
		super.finalize();
	}

	@Override
	public void onShutdown(Node node) {
		try {
			finalize();
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onShutdownComplete(Node node) {
	}

	@Override
	public void onError(Node node, Throwable throwable) {
	}

}