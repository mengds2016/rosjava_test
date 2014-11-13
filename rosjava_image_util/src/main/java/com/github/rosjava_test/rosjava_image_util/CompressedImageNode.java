package com.github.rosjava_test.rosjava_image_util;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;

import javax.imageio.ImageIO;

import org.jboss.netty.buffer.ChannelBuffer;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Publisher;
import org.ros.node.topic.Subscriber;


public class CompressedImageNode extends AbstractNodeMain {

	private Publisher<std_msgs.String> status_publisher ;

	private String nodeName = "rosjava_test/compressed_image_node" ;
	private String raw_topic_name = this.nodeName + "/raw";
	private String com_topic_name = this.nodeName + "/compressed";
	
	public CompressedImageNode ( String nodeName, String raw_topic_name, String com_topic_name ){
		super();
		if ( nodeName != null ) this.nodeName = nodeName ;
		if ( raw_topic_name != null ) this.raw_topic_name = raw_topic_name;
		if ( com_topic_name != null ) this.com_topic_name = com_topic_name;
	}
	
	@Override
	public GraphName getDefaultNodeName() {
		return GraphName.of(this.nodeName);
	}

	@Override
	public void onStart(final ConnectedNode connectedNode) {

		this.status_publisher = connectedNode.newPublisher(this.nodeName + "/status/string", std_msgs.String._TYPE);
		
		Subscriber<sensor_msgs.Image> raw_image_subscriber = connectedNode.newSubscriber(
				this.raw_topic_name, sensor_msgs.Image._TYPE);
		raw_image_subscriber.addMessageListener(new MessageListener<sensor_msgs.Image>() {
			@Override
			public void onNewMessage(sensor_msgs.Image image) {
				if ( !image.getEncoding().contains("rgb8") ){
					System.out.println( "[" + CompressedImageNode.this.nodeName + "] invalid encoding " + image.getEncoding() ) ;
					return ;
				}
				try {
					BufferedImage buf = new BufferedImage(image.getWidth(),image.getHeight(),BufferedImage.TYPE_INT_RGB) ;
					ChannelBuffer buffer = image.getData();
					byte[] data = buffer.array() ;
					for ( int i=0 ; i<image.getWidth() ; i++ ){
						for  ( int j=0 ; j<image.getHeight() ; j++ ){
							int pos = 3 * ( i + j * image.getWidth() ) ;
							int rgb = (((int) data[pos + 0] << 16) & 0xFF0000) 
									 + (((int) data[pos + 1] << 8)  & 0x00FF00)
									 + (((int) data[pos + 2] << 0)  & 0x0000FF) ;
							buf.setRGB(i, j, rgb) ;
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}, 1);
		
		Subscriber<sensor_msgs.CompressedImage> com_image_subscriber = connectedNode.newSubscriber(
				this.com_topic_name, sensor_msgs.CompressedImage._TYPE);
		com_image_subscriber.addMessageListener(new MessageListener<sensor_msgs.CompressedImage>() {
			@Override
			public void onNewMessage(sensor_msgs.CompressedImage image) {
				try {
					ChannelBuffer buffer = image.getData();
					byte[] data = buffer.array() ;
					String sData = new String(data) ;
					int start = sData.indexOf("JFIF");
					if ( start > 6 ) start -= 6 ;
					if ( start > 0 ){
						System.out.println(" jpeg header detected "
								+ start);
						InputStream bais = new ByteArrayInputStream(buffer.array());  
						BufferedImage buf = ImageIO.read(bais);
						ImageIO.write(buf, "test.jpg", new File("test.jpg"));
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}, 1);
	}

	
	//File tmp = new File("tmp");
	//write(tmp.getAbsolutePath(), buffer.array(),
	//		start);
//	public static void write(String path, byte[] data, int offset) {
//		try {
//			OutputStream out = new FileOutputStream(path);
//			for ( int i=0 ; i<offset ; i++ ){
//				if ( offset == data[i] ) {
//					System.out.println(i) ;
//				}
//			}
//			out.write(data, offset, data.length - offset);
//			out.close();
//		} catch (FileNotFoundException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
	
	public static BufferedImage monoImage(byte[] data, int w, int h) {
		BufferedImage buf = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		//int step = data.length / (w * h);
		int offset = data.length - w * h;
		System.out.println(offset);
		for (int i = 0; i < w; i++) {
			for (int j = 0; j < h; j++) {
				int val = 0;
				// for (int k = step - 1; k >= 0; k--) {
				// val = val << 8;
				// val = (val + data[k + step * (i + j * w)]);
				// }
				val = data[offset + (i + j * w)] & 0xff;
				int col = val << 16 | val << 8 | val;
				buf.setRGB(i, j, col);
			}
		}
		return buf;
	}
}