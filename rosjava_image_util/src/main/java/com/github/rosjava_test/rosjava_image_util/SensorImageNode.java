package com.github.rosjava_test.rosjava_image_util;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;

import javax.imageio.ImageIO;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Publisher;
import org.ros.node.topic.Subscriber;


public class SensorImageNode extends AbstractNodeMain {

	protected String nodeName = "sensor_image_node" ;
	
	protected ArrayList<String> name_space_array ;
	protected HashMap<String, SensorImageTopics> image_topics_hash;
	
	public SensorImageNode (){
		this("sensor_image_node");
	}
	
	public SensorImageNode(String nodeName){
		this(nodeName, null);
	}
	
	public SensorImageNode (String nodeName, ArrayList<String> name_space_array){
		super();
		updateTopics(nodeName, name_space_array);
	}
	
	public void updateTopics (String nodeName, ArrayList<String> name_space_array){
		this.nodeName = nodeName;
		if ( name_space_array == null ){
			name_space_array = new ArrayList<String>();
			name_space_array.add(nodeName);
		}
		this.name_space_array = name_space_array ;
		this.image_topics_hash = new HashMap<String, SensorImageTopics>();
		for ( String name : this.name_space_array) {
			SensorImageTopics topics = new SensorImageTopics(name);
			this.image_topics_hash.put(name, topics);
		}
	}
	
	@Override
	public GraphName getDefaultNodeName() {
		return GraphName.of(this.nodeName);
	}

	@Override
	public void onStart(final ConnectedNode connectedNode) {

		for (final String name : this.name_space_array) {
			SensorImageTopics topic = this.image_topics_hash.get(name);
			topic.onStart(connectedNode);

			topic.command_subscriber
					.addMessageListener(new MessageListener<std_msgs.String>() {
						@Override
						public void onNewMessage(std_msgs.String arg0) {
							stringFunction(arg0.getData(), name);
						}
					});

			topic.raw_image_subscriber.addMessageListener(
					new MessageListener<sensor_msgs.Image>() {
						@Override
						public void onNewMessage(sensor_msgs.Image image) {
							if (!image.getEncoding().contains("rgb8")) {
								System.out.println("["
										+ SensorImageNode.this.nodeName
										+ "] invalid encoding "
										+ image.getEncoding());
								return;
							}
							try {
								BufferedImage buf = new BufferedImage(image
										.getWidth(), image.getHeight(),
										BufferedImage.TYPE_INT_RGB);
								ChannelBuffer buffer = image.getData();
								byte[] data = buffer.array();
								for (int i = 0; i < image.getWidth(); i++) {
									for (int j = 0; j < image.getHeight(); j++) {
										int pos = 3 * (i + j * image.getWidth());
										int rgb = (((int) data[pos + 0] << 16) & 0xFF0000)
												+ (((int) data[pos + 1] << 8) & 0x00FF00)
												+ (((int) data[pos + 2] << 0) & 0x0000FF);
										buf.setRGB(i, j, rgb);
									}
								}
								rawImageFunction(buf, name);
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					}, 1);

			topic.com_image_subscriber.addMessageListener(
					new MessageListener<sensor_msgs.CompressedImage>() {
						@Override
						public void onNewMessage(
								sensor_msgs.CompressedImage image) {
							try {
								ChannelBuffer buffer = image.getData();
								byte[] data = buffer.array();
								String sData = new String(data);
								int start = sData.indexOf("JFIF");
								if (start > 6)
									start -= 6;
								if (start > 0) {
									System.out.println(" jpeg header detected "
											+ start);
									InputStream bais = new ByteArrayInputStream(
											data, start, data.length - start);
									BufferedImage buf = ImageIO.read(bais);
									comImageFunction(buf, name);
								}
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					}, 1);
		}
	}

	protected void rawImageFunction(BufferedImage buf, String tag){};
	protected void comImageFunction(BufferedImage buf, String tag){};
	protected void stringFunction(String buf, String tag){};

	public void publishCompressedImage(String path) throws IOException{
		byte[] b = new byte[1024];
	    FileInputStream fis = new FileInputStream(path);
	    ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    while (fis.read(b) > 0) {
	        baos.write(b);
	    }
	    baos.close();
	    fis.close();
	    b = baos.toByteArray();
	    publishCompressedImage(b);
	}

	public void publishCompressedImage(byte[] data){
		publishCompressedImage(data, this.nodeName);
	}
	
	public void publishCompressedImage(byte[] data, String tag){
		SensorImageTopics topic = this.image_topics_hash.get(tag);
		if ( tag == null ) {
			System.out.println("[" + this.nodeName + "] unknown topic name " + tag) ;
			return;
		}
		sensor_msgs.CompressedImage image_topic = topic.com_image_publisher.newMessage();
		image_topic.setData(ChannelBuffers.copiedBuffer(ByteOrder.LITTLE_ENDIAN, data, 0, data.length));
		topic.com_image_publisher.publish(image_topic) ;
	}
	
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
	
	
	public class SensorImageTopics{
		
		public Publisher<std_msgs.String> status_publisher ;
		public Publisher<sensor_msgs.CompressedImage> com_image_publisher ;
		public Subscriber<std_msgs.String> command_subscriber;
		public Subscriber<sensor_msgs.Image> raw_image_subscriber;
		public Subscriber<sensor_msgs.CompressedImage> com_image_subscriber;
		
		public String nodeName;
		public String raw_image_sub_topic_name;
		public String com_image_sub_topic_name;
		public String status_string_topic_name;
		public String com_image_pub_topic_name;
		public String command_string_topic_name;
		
		public SensorImageTopics (){
			this("sensor_image_node");
		}
		
		public SensorImageTopics (String nodeName){
			super();
			updateTopicName(nodeName);
		}
		
		protected void updateTopicName(String nodeName){
			this.nodeName = nodeName ;
			this.raw_image_sub_topic_name = this.nodeName + "/image/in/raw";
			this.com_image_sub_topic_name = this.nodeName + "/image/in/compressed";
			this.status_string_topic_name = this.nodeName + "/status/string";
			this.com_image_pub_topic_name = this.nodeName + "/image/out/compressed";
			this.command_string_topic_name = this.nodeName + "/command/string";
		}

		protected void updateTopicNameFromRosParam(final ConnectedNode connectedNode) {
			this.raw_image_sub_topic_name = connectedNode.getParameterTree().getString(
					this.nodeName + "/raw_image_sub_topic_name", this.raw_image_sub_topic_name);
			this.com_image_sub_topic_name = connectedNode.getParameterTree().getString(
					this.nodeName + "/com_image_sub_topic_name", this.com_image_sub_topic_name);
			this.status_string_topic_name = connectedNode.getParameterTree().getString(
					this.nodeName + "/status_string_topic_name", this.status_string_topic_name);
			this.com_image_pub_topic_name = connectedNode.getParameterTree().getString(
					this.nodeName + "/com_image_pub_topic_name", this.com_image_pub_topic_name);
			this.command_string_topic_name = connectedNode.getParameterTree().getString(
					this.nodeName + "/command_string_topic_name", this.command_string_topic_name);
		}

		protected void updateTopicNameFromEnv() {
			String buf;
			buf = System.getenv("raw_image_sub_topic_name");
			if ( buf != null ) this.raw_image_sub_topic_name = buf;
			buf = System.getenv("com_image_sub_topic_name");
			if ( buf != null ) this.com_image_sub_topic_name = buf;
			buf = System.getenv("status_string_topic_name");
			if ( buf != null ) this.status_string_topic_name = buf;
			buf = System.getenv("com_image_pub_topic_name");
			if ( buf != null ) this.com_image_pub_topic_name = buf;
			buf = System.getenv("command_string_topic_name");
			if ( buf != null ) this.command_string_topic_name = buf;
		}

		
		public void onStart(final ConnectedNode connectedNode) {
			// updateTopicNameFromRosParam(connectedNode);
			updateTopicNameFromEnv();
			this.status_publisher = connectedNode.newPublisher(this.status_string_topic_name, std_msgs.String._TYPE);
			this.com_image_publisher = connectedNode.newPublisher(this.com_image_pub_topic_name, sensor_msgs.CompressedImage._TYPE);
			this.command_subscriber =  connectedNode.newSubscriber(this.command_string_topic_name, std_msgs.String._TYPE);
			this.raw_image_subscriber = connectedNode.newSubscriber(this.raw_image_sub_topic_name, sensor_msgs.Image._TYPE);
			this.com_image_subscriber = connectedNode.newSubscriber(this.com_image_sub_topic_name, sensor_msgs.CompressedImage._TYPE);
		}
	}
	
}