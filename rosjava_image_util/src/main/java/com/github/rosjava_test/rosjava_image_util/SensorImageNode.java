package com.github.rosjava_test.rosjava_image_util;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.AffineTransform;
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
	
	protected ConnectedNode connectedNode;
	
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
		this.connectedNode = connectedNode;
		
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
							int imageType = BufferedImage.TYPE_INT_RGB;
							if (image.getEncoding().contains("rgb8")){
							} else if (image.getEncoding().contains("bgr8")){
								imageType = BufferedImage.TYPE_INT_BGR;
							} else {
								System.out.println("["
										+ SensorImageNode.this.nodeName
										+ "] invalid encoding "
										+ image.getEncoding());
								return;
							}
							try {
								BufferedImage buf = new BufferedImage(image
										.getWidth(), image.getHeight(), imageType);
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
								int start;
								if ((start = sData.indexOf("JFIF")) > 6){
									start -= 6;
									System.out.println(" jpeg header detected " + start);
								} else if ((start = sData.indexOf("PNG")) > 1){
									start -= 1;
									System.out.println(" png header detected " + start);
								}
								if (start > 0) {
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
	
	public void publishRawImage(BufferedImage image, String tag){
		SensorImageTopics topic = this.image_topics_hash.get(tag);
		if ( tag == null ) {
			System.out.println("[" + this.nodeName + "] unknown topic name " + tag) ;
			return;
		}
		sensor_msgs.Image image_topic = topic.raw_image_publisher.newMessage();
		///image_topic.setData(ChannelBuffers.copiedBuffer(ByteOrder.LITTLE_ENDIAN, image, 0, data.length));
		//topic.raw_image_publisher.publish(image_topic) ;
	}
	
	public static BufferedImage monoImage(byte[] data, int w, int h) {
		BufferedImage buf = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		int offset = data.length - w * h;
		System.out.println(offset);
		for (int i = 0; i < w; i++) {
			for (int j = 0; j < h; j++) {
				int val = 0;
				val = data[offset + (i + j * w)] & 0xff;
				int col = val << 16 | val << 8 | val;
				buf.setRGB(i, j, col);
			}
		}
		return buf;
	}
	
	public static BufferedImage resizeImage(BufferedImage image, int width, int height, int rotate) {
		BufferedImage thumb=null;
		if ( rotate % 2 == 0 ){
			thumb = new BufferedImage(width, height, image.getType());
		} else{
			thumb = new BufferedImage(height, width, image.getType());
		}			
		AffineTransform at = new AffineTransform();
        at.translate(width / 2, height / 2);
		at.rotate(Math.PI / 2 * rotate);
		at.scale(width * 1.0 / image.getWidth(),
				height * 1.0 / image.getHeight());
		at.translate(-image.getWidth()/2, -image.getHeight()/2);
		Graphics2D g2d = (Graphics2D) thumb.getGraphics();
		g2d.drawImage(image, at, null);
		return thumb;
	}
	
	public String getStringParameterEnvOrRos(String tag, String defo){
		String ret;
		ret = System.getenv(tag);
		if ( ret == null ){
			ret = this.connectedNode.getParameterTree().getString(tag,defo);
		}
		return ret;
	}
	
	public double getDoubleParameterEnvOrRos(String tag, double defo){
		double ret;
		try{
			ret = Double.parseDouble(System.getenv(tag));
		} catch ( Exception e ){
			ret = this.connectedNode.getParameterTree().getDouble(tag,defo);
		}
		return ret;
	}
	
	public int getIntegerParameterEnvOrRos(String tag, int defo){
		int ret;
		try{
			ret = Integer.parseInt(System.getenv(tag));
		} catch ( Exception e ){
			ret = this.connectedNode.getParameterTree().getInteger(tag,defo);
		}
		return ret;
	}
	
	public class SensorImageTopics{
		
		public Publisher<std_msgs.String> status_publisher ;
		public Publisher<sensor_msgs.CompressedImage> com_image_publisher ;
		public Publisher<sensor_msgs.Image> raw_image_publisher ;

		public Subscriber<std_msgs.String> command_subscriber;
		public Subscriber<sensor_msgs.Image> raw_image_subscriber;
		public Subscriber<sensor_msgs.CompressedImage> com_image_subscriber;
		
		public String nodeName;
		public String raw_image_sub_topic_name;
		public String com_image_sub_topic_name;
		public String status_string_topic_name;
		public String com_image_pub_topic_name;
		public String raw_image_pub_topic_name;
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
			this.raw_image_pub_topic_name = this.nodeName + "/image/out/raw";
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
			this.raw_image_pub_topic_name = connectedNode.getParameterTree().getString(
					this.nodeName + "/raw_image_pub_topic_name", this.raw_image_pub_topic_name);
			this.command_string_topic_name = connectedNode.getParameterTree().getString(
					this.nodeName + "/command_string_topic_name", this.command_string_topic_name);
		}

		protected void updateTopicNameFromEnv() {
			String buf;
			buf = System.getenv(this.nodeName + "/raw_image_sub_topic_name");
			if ( buf != null ) this.raw_image_sub_topic_name = buf;
			buf = System.getenv(this.nodeName + "/com_image_sub_topic_name");
			if ( buf != null ) this.com_image_sub_topic_name = buf;
			buf = System.getenv(this.nodeName + "/status_string_topic_name");
			if ( buf != null ) this.status_string_topic_name = buf;
			buf = System.getenv(this.nodeName + "/com_image_pub_topic_name");
			if ( buf != null ) this.com_image_pub_topic_name = buf;
			buf = System.getenv(this.nodeName + "/raw_image_pub_topic_name");
			if ( buf != null ) this.raw_image_pub_topic_name = buf;
			buf = System.getenv(this.nodeName + "/command_string_topic_name");
			if ( buf != null ) this.command_string_topic_name = buf;
		}

		
		public void onStart(final ConnectedNode connectedNode) {
			updateTopicNameFromRosParam(connectedNode);
			updateTopicNameFromEnv();
			this.status_publisher = connectedNode.newPublisher(this.status_string_topic_name, std_msgs.String._TYPE);
			this.com_image_publisher = connectedNode.newPublisher(this.com_image_pub_topic_name, sensor_msgs.CompressedImage._TYPE);
			this.raw_image_publisher = connectedNode.newPublisher(this.raw_image_pub_topic_name, sensor_msgs.Image._TYPE);
			this.command_subscriber =  connectedNode.newSubscriber(this.command_string_topic_name, std_msgs.String._TYPE);
			this.raw_image_subscriber = connectedNode.newSubscriber(this.raw_image_sub_topic_name, sensor_msgs.Image._TYPE);
			this.com_image_subscriber = connectedNode.newSubscriber(this.com_image_sub_topic_name, sensor_msgs.CompressedImage._TYPE);
		}
	}
	
}