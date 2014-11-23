package com.github.rosjava_test.rosjava_image_util;

import java.awt.image.BufferedImage;
import java.util.ArrayList;

import org.ros.node.ConnectedNode;

import com.github.rosjava_test.rosjava_image_util.ImageWindowSampleFrame.ImageData;

public class ImageWindowNode extends SensorImageNode{

	private ImageWindowSampleFrame window;
	
	public ImageWindowNode (){
		super("image_window_node");
		this.window = new ImageWindowSampleFrame();
	}
	
	@Override
	public void onStart(final ConnectedNode connectedNode) {
		String nodeName = connectedNode.getParameterTree().getString(
				"ROSJAVA_IMAGE_UTIL_WINDOW_NODE_NAME",
				this.nodeName); 
		
		ArrayList<String> name_space_array = new ArrayList<String>();
		name_space_array.add("/image_window_node/left");
		name_space_array.add("/image_window_node/right");
		updateTopics(nodeName,name_space_array);
		
		for (ImageData d : this.window.getImageDataList()){
			d.rect_publisher = connectedNode.newPublisher("/image_window_node/" + d.name + "/rect", std_msgs.Int32MultiArray._TYPE);
		}
		
		super.onStart(connectedNode);
	}

	@Override
	protected void rawImageFunction(BufferedImage buf, String tag){
		if ( this.window != null && buf != null ) {
			if ( tag.contains("left")){
				this.window.setLeftImage(buf);
			} else if ( tag.contains("right")){
				this.window.setRightImage(buf);
			}
		}
	};
	
	@Override
	protected void comImageFunction(BufferedImage buf, String tag){
		rawImageFunction(buf,tag);
	};

	@Override
	protected void stringFunction(String buf, String tag){
	};

}
