package com.github.rosjava_test.rosjava_audio_util;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import org.jboss.netty.buffer.ChannelBuffer;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.topic.Subscriber;

public class SpeakerNode extends AbstractNodeMain {

	private SourceDataLine sourceDataLine;
	private MicTest mic;
	
	@Override
	public GraphName getDefaultNodeName() {
		return GraphName.of("rosjava_test/speaker");
	}
	
	@Override
	public void onShutdown(Node node){
		if ( this.sourceDataLine != null ){
			this.sourceDataLine.close();
		}
	}

	@Override
	public void onStart(ConnectedNode connectedNode) {

		this.mic = new MicTest();
		try {
			this.sourceDataLine = mic.setupSource();
		} catch (LineUnavailableException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		
		String request_topic = connectedNode.getParameterTree().getString(
				"ROSJAVA_VOICE_SPEAKER_REQUEST_TOPIC",
				"rosjava_test/speaker/request");
		
		Subscriber<audio_common_msgs.AudioData> subscriber = connectedNode
				.newSubscriber(request_topic,
						audio_common_msgs.AudioData._TYPE);
		subscriber
				.addMessageListener(new MessageListener<audio_common_msgs.AudioData>() {
					@Override
					public void onNewMessage(audio_common_msgs.AudioData message) {
						ChannelBuffer ch = message.getData();
						byte[] buffer = ch.array();
						SpeakerNode.this.sourceDataLine.write(buffer, ch.arrayOffset(), ch.readableBytes());
						// mic.syncWrite(buffer, ch.arrayOffset(), ch.readableBytes());
					}
				});
	}
}
