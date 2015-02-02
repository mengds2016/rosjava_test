package org.ros.android.chat;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteOrder;

import org.jboss.netty.buffer.ChannelBuffers;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Publisher;

import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;


public class ImagePublishNode extends AbstractNodeMain implements PreviewCallback, Runnable, Camera.ShutterCallback {

	private sensor_msgs.CompressedImage image_topic ;
	private Publisher<sensor_msgs.CompressedImage> image_publisher ;
	private String topic_name = RosChatActivity.node_name + "/status/camera/image/compressed";
	private Camera camera = null;
	private int width, height ;
	private double hz = 2.5;
	private Thread thread = null;
	private boolean started = false ;
	private int rotate_cnt ;
	
	private ByteArrayOutputStream baos = new ByteArrayOutputStream();
	private boolean camera_publish_ready = false;
	
	@Override
	public GraphName getDefaultNodeName() {
		return GraphName.of( RosChatActivity.node_name + "/camera_compressed_image_publisher");
	}

	@Override
	public void onStart(final ConnectedNode connectedNode) {
		this.image_publisher = connectedNode.newPublisher(
				this.topic_name, sensor_msgs.CompressedImage._TYPE);
		this.image_topic = connectedNode.getTopicMessageFactory().newFromType(
				sensor_msgs.CompressedImage._TYPE);
		this.image_topic.setFormat("jpeg") ;
		this.started = true ;
	}
	
	public void setRotateCnt(int rc){
		this.rotate_cnt = (rc+1) % 4 ;
	}

	public void startImagePublisher(Camera cam, int width, int height ){
		if ( this.camera == null && this.thread == null ){
			System.out.println("startImagePublisher");
			this.camera = cam;
			this.width = width;
			this.height = height;
			this.thread = new Thread(this);
			// this.camera.startPreview() ;
			this.thread.start();
		} else {
			System.out.println("call startImagePublisher several times...?");
		}
	}
	
	public void stopImagePublisher(){
		this.camera = null ;
		this.thread = null ;
		try {
			Thread.sleep((long)(1000.0 / this.hz)) ;
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void run(){
		long start ;
		long end ;
		long sleep = (long)(1000.0 / this.hz) ;
		while (true) {
			//System.out.println( "[ImagePublisher] running" ) ;
			try {
				start = System.currentTimeMillis();
				if ( this.camera_publish_ready ){
					byte[] data;
					synchronized(this.baos){
						data = this.baos.toByteArray();
					}
					publishCompressedCameraImage(data);
				}
				//if (this.camera == null ) break ;
				try {
					this.camera.setOneShotPreviewCallback(this);
				} catch (NullPointerException e) {
					break;
				}
				//this.camera.takePicture(this, null, this) ;
				end = System.currentTimeMillis();
				if (end - start < sleep) {
					Thread.sleep(sleep - (end - start));
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	@Override
	public void onPreviewFrame(byte[] data, Camera camera) {
		synchronized (this.baos) {
			try {
				this.baos.reset();
				this.baos.write(data);
				this.camera_publish_ready = true;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
        
//        Bitmap bmap = BitmapFactory.decodeByteArray(baos.toByteArray(), 0, baos.toByteArray().length) ;
//        Matrix matrix = new Matrix();
//       // matrix.postScale(1, 1);
//        matrix.postRotate(this.rotate_cnt*-90);
//		bmap = Bitmap.createBitmap(bmap, 0, 0, this.width, this.height, matrix,
//				true);
//		baos.reset();
//        bmap.compress(Bitmap.CompressFormat.JPEG, 50, baos);
        // this.onPictureTaken(this.baos.toByteArray(), camera) ;

       //  bmap.recycle();
        // baos.close();
	}
	
	public void publishCompressedCameraImage(byte[] data) {
		if ( this.image_publisher != null && this.image_topic != null && this.started){
			byte[] yuv = data;
			YuvImage yuvimage = new YuvImage(yuv, ImageFormat.NV21, this.width,
					this.height, null);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			yuvimage.compressToJpeg(new Rect(0, 0, this.width, this.height),
					50, baos);
			yuvimage = null;
			data = baos.toByteArray();
			this.image_topic.setData(ChannelBuffers.copiedBuffer(
					ByteOrder.LITTLE_ENDIAN, data, 0, data.length));
			this.image_publisher.publish(this.image_topic);
        }
	}

//	@Override
//	public void onPictureTaken(byte[] data, Camera camera) {
//		//System.out.println("[onPictureTaken] jpeg image receive!") ;
//		if ( this.image_publisher != null && this.image_topic != null && this.started){
//			
////			if ( this.image_topic.getData().capacity() <= data.length ){
////				System.out.print( "[ImagePublishNode]" + this.image_topic.getData().capacity());
////				this.image_topic.setData(ChannelBuffers.dynamicBuffer(ByteOrder.LITTLE_ENDIAN,data.length+1)) ;
////				System.out.println( " -> " + this.image_topic.getData().capacity());
////			}
////			this.image_topic.getData().setBytes(0, data, 0, data.length) ;
//			
//			this.image_topic.setData(ChannelBuffers.copiedBuffer(ByteOrder.LITTLE_ENDIAN, data, 0, data.length));
//			
////        		ChannelBuffer buf = this.image_topic.getData();
////            	buffOutStream = new ChannelBufferOutputStream(buf);
////				buffOutStream.write(data) ;
////				buffOutStream.close() ;
////			} catch (IOException e) {
////				e.printStackTrace();
////			} 
//        	this.image_publisher.publish(this.image_topic) ;
//        }
//	}

	@Override
	public void onShutter() {
		System.out.println("[onPictureTaken] skip shutter") ;
	}
}
