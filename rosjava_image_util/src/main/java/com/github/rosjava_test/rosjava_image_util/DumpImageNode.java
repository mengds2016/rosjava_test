package com.github.rosjava_test.rosjava_image_util;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.ros.node.ConnectedNode;

public class DumpImageNode extends SensorImageNode{

	public DumpImageNode (){
		super();
	}
	
	@Override
	public void onStart(final ConnectedNode connectedNode) {
		String nodeName = connectedNode.getParameterTree().getString(
				"ROSJAVA_IMAGE_UTIL_DUMP_NODE_NAME",
				this.nodeName); 
		updateTopics(nodeName, null);
		super.onStart(connectedNode);
	}
	
	@Override
	protected void rawImageFunction(BufferedImage buf, String tag){
		try {
			ImageIO.write(buf, "jpeg", new File("/tmp/test_raw.jpg"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	};
	
	@Override
	protected void comImageFunction(BufferedImage buf, String tag){
		try {
			ImageIO.write(buf, "jpeg", new File("/tmp/test_com.jpg"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	};

	@Override
	protected void stringFunction(String buf, String tag){
		SensorImageTopics topic = this.image_topics_hash.get(tag);
		System.out.println("  | publish " + buf + " --> " + topic.com_image_publisher.getTopicName());
		try {
			publishCompressedImage(buf);
		} catch (IOException e) {
			e.printStackTrace();
		}
	};

}
