package com.github.rosjava_test.rosjava_image_util;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

public class DumpImageNode extends SensorImageNode{

	public DumpImageNode (){
		this(null,null,null);
	}
	
	public DumpImageNode(String nodeName, String raw_topic_name,
			String com_topic_name) {
		super(nodeName, raw_topic_name, com_topic_name);
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

}
