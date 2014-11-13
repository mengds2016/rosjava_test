package com.github.rosjava_test.rosjava_image_util;

import java.awt.image.BufferedImage;

import org.ros.node.ConnectedNode;

public class ImageWindowNode extends SensorImageNode{

	private ImageWindowSampleFrame window;
	
	public ImageWindowNode (){
		super("image_window_node",null,null);
		this.window = new ImageWindowSampleFrame();
	}
	
	@Override
	public void onStart(final ConnectedNode connectedNode) {
		String nodeName = connectedNode.getParameterTree().getString(
				"ROSJAVA_IMAGE_UTIL_WINDOW_NODE_NAME",
				this.nodeName); 
		updateTopicName(nodeName,null,null);
		super.onStart(connectedNode);
	}

	@Override
	protected void rawImageFunction(BufferedImage buf){
		if ( this.window != null && buf != null ) {
			this.window.updateImage(buf);
		}
	};
	
	@Override
	protected void comImageFunction(BufferedImage buf){
		if ( this.window != null && buf != null ) {
			this.window.updateImage(buf);
		}
	};

	@Override
	protected void stringFunction(String buf){
	};

}
