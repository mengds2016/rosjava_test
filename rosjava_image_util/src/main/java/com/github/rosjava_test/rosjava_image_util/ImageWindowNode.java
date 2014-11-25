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
		name_space_array.add(nodeName+"/left");
		name_space_array.add(nodeName+"/right");
		name_space_array.add(nodeName+"/right/overlay");
		updateTopics(nodeName,name_space_array);
		
		this.window.file_receive_pub = connectedNode.newPublisher(this.nodeName+"/drop_file/path", std_msgs.String._TYPE);
		
		for (ImageData d : this.window.getImageDataList()){
			d.rect_publisher = connectedNode.newPublisher(nodeName + "/" + d.name + "/rect", std_msgs.Int32MultiArray._TYPE);
			d.rect_normal_publisher = connectedNode.newPublisher(nodeName + "/" + d.name + "/rect/normalize", std_msgs.Float32MultiArray._TYPE);
		}
		
		super.onStart(connectedNode);
	}

	@Override
	protected void rawImageFunction(BufferedImage buf, String tag){
		if ( this.window != null && buf != null ) {
			if (tag.contains("overlay")){
				this.window.rightCameraView.pane.setOverlayImage(buf);
				// System.out.println(" overlay image = " + buf);
			} else if ( tag.contains("left")){
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
