package com.github.rosjava_test.rosjava_image_util;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;

import org.ros.node.ConnectedNode;

public class ImageRepublishNode extends SensorImageNode{

	private ConnectedNode connectedNode;
	private double max_pub_rate = 3; // hz
	private double scale = 1.0 ;
	private long last_publish_time = System.currentTimeMillis();
	
	public ImageRepublishNode (){
		super();
	}
	
	@Override
	public void onStart(final ConnectedNode connectedNode) {
		this.connectedNode = connectedNode;
		
		String nodeName = getStringParameterEnvOrRos(
				"ROSJAVA_IMAGE_REPUBLISH_NODE_NAME", this.nodeName);
		ArrayList<String> name_space_array = null;
		//		new ArrayList<String>();
		// name_space_array.add(nodeName+"/left");
		updateTopics(nodeName,name_space_array);
		
		this.max_pub_rate = getDoubleParameterEnvOrRos("ROSJAVA_IMAGE_REPUBLISH_MAX_RATE", this.max_pub_rate);
		this.scale = getDoubleParameterEnvOrRos("ROSJAVA_IMAGE_REPUBLISH_SCALE", this.scale);
		
		super.onStart(connectedNode);
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

	public void republishImage(BufferedImage img){
		// this.max_pub_rate = this.connectedNode.getParameterTree().getDouble("ROSJAVA_IMAGE_REPUBLISH_MAX_RATE", this.max_pub_rate);
		// this.scale = this.connectedNode.getParameterTree().getDouble("ROSJAVA_IMAGE_REPUBLISH_SCALE", this.scale);
		//
		//
		if ( System.currentTimeMillis() - this.last_publish_time > 1000.0/this.max_pub_rate ){
			System.out.println("["+this.nodeName+"]" + " x" + this.scale + " in " + (System.currentTimeMillis() - this.last_publish_time) + "ms" );
			if ( this.scale != 1.0 ){
				img = SensorImageNode.resizeImage(img, (int)(img.getWidth()*this.scale), (int)(img.getHeight()*this.scale));
			}
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try {
				ImageIO.write(img, "jpg", baos);
				byte[] bytes = baos.toByteArray();
				publishCompressedImage(bytes);
				this.last_publish_time += (long)(1000.0/this.max_pub_rate);
				if ( System.currentTimeMillis() - this.last_publish_time > 1000.0/this.max_pub_rate ){
					this.last_publish_time = System.currentTimeMillis();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			try {
				Thread.sleep((long)(1000.0/this.max_pub_rate * 0.5));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	@Override
	protected void rawImageFunction(BufferedImage buf, String tag){
		if ( buf != null )
			republishImage(buf);
	};
	
	@Override
	protected void comImageFunction(BufferedImage buf, String tag){
		if ( buf != null )
			republishImage(buf);
	};

	@Override
	protected void stringFunction(String buf, String tag){
	};

}
