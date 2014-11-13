package com.github.rosjava_test.rosjava_image_util;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.ros.node.ConnectedNode;

public class DumpImageNode extends SensorImageNode{

	public DumpImageNode (){
		super(null,null,null);
	}
	
	@Override
	public void onStart(final ConnectedNode connectedNode) {
		String nodeName = connectedNode.getParameterTree().getString(
				"ROSJAVA_IMAGE_UTIL_DUMP_NODE_NAME",
				this.nodeName); 
		updateTopicName(nodeName,null,null);
		super.onStart(connectedNode);
	}
	
	@Override
	protected void rawImageFunction(BufferedImage buf){
		try {
			ImageIO.write(buf, "jpeg", new File("/tmp/test_raw.jpg"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	};
	
	@Override
	protected void comImageFunction(BufferedImage buf){
		try {
			ImageIO.write(buf, "jpeg", new File("/tmp/test_com.jpg"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	};

	@Override
	protected void stringFunction(String buf){
		System.out.println("  | publish " + buf + " --> " + this.com_image_publisher.getTopicName());
		try {
			publishCompressedImage(buf);
		} catch (IOException e) {
			e.printStackTrace();
		}
	};

}
