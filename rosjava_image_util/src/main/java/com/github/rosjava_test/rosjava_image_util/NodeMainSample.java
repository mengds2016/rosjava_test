package com.github.rosjava_test.rosjava_image_util;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;

import org.ros.node.DefaultNodeMainExecutor;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMain;
import org.ros.node.NodeMainExecutor;

public class NodeMainSample {

	public static 	HashMap<String,String> remap ;

	public static String remapTopic( String topic){
		HashMap<String,String> rm = remap ;
		if ( rm == null ){
			remap = new HashMap<String,String>() ;
			rm = remap ;
		}
		return remapTopic(topic,rm) ;
	}
	
	public static String remapTopic( String topic, HashMap<String,String> remap ){
		String ret = topic ;
		String buf ;
		if ( (buf = remap.get(topic)) != null || (buf = remap.get("/" + topic)) != null){
			ret = buf ;
		}
		return ret ;
	}
	
	public void runNode(String[] args, NodeMain nodeMain) throws URISyntaxException{
		String ros_ip = null, ros_master = null, camera_topic = null, node_name = null ;
		remap = new HashMap<String,String>();
		
		char mode = 'w';
		for (String buf : args) {
			if (buf.length() == 0) {
				System.out.println("null arg");
			} else if (buf.charAt(0) == '-' && buf.length() > 1) {
				System.out.println("option detected " + buf);
				mode = buf.charAt(1);
				if (mode == 'h') {
					System.out
							.println("[usage] command (-r ros_ip) (-m ros_master)");
					System.exit(0);
				}
			} else {
				switch (mode) {
				case 'w':
					System.out.println("skip " + buf);
					break;
				case 'r':
					System.out.println("ros_ip " + buf);
					ros_ip = buf;
					break;
				case 'm':
					System.out.println("ros_master " + buf);
					ros_master = buf;
					break;
				case 'n':
					System.out.println("node_name " + buf);
					node_name = buf;
					break;
				case 't':
					System.out.println("remap " + buf);
					String[] split = buf.split(":=") ;
					if ( split.length > 1 ){
						String key = split[0].trim() ;
						String val = split[1].trim() ;
						remap.put( key, val ) ;
						System.out.println("      " + key + " -> " + val );
					} else {
						System.out.println("camera_topic " + buf);
						camera_topic = buf;
					}
					mode = 'w' ;
					break;
				default:
					System.out.println("unknow tag " + buf);
				}
			}
		}

		if (ros_ip == null)
			ros_ip = "127.0.0.1" ; //System.getenv("ROS_IP") ;
		if (ros_master == null)
			ros_master = "http://" + ros_ip + ":11311" ; //System.getenv("ROS_MASTER_URI") ;
		if (camera_topic == null )
			camera_topic = "/kubi_sample/camera_compressed_image" ;
		if (node_name == null)
			node_name = "kubi_sample" ;

		NodeConfiguration nodeConfiguration = NodeConfiguration.newPublic(
				ros_ip, new URI(ros_master));
		NodeMainExecutor runner = DefaultNodeMainExecutor.newDefault();
		runner.execute(nodeMain, nodeConfiguration);
	}

	public static void main(String[] args) throws Exception {
		// runNode(args, hoge)
	}

}
