package mouse_controller;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;

import org.ros.node.DefaultNodeMainExecutor;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

public class NodeMain {

	public static HashMap<String, String> remap = new HashMap<String, String>();
	private static String ros_ip = "192.168.96.214", ros_master = "http://192.168.96.214:11311", node_name = "";

	public static String remapTopic(String topic) {
		HashMap<String, String> rm = remap;
		if (rm == null) {
			remap = new HashMap<String, String>();
			rm = remap;
		}
		return remapTopic(topic, rm);
	}

	public static String remapTopic(String topic, HashMap<String, String> remap) {
		String ret = topic;
		String buf;
		if ((buf = remap.get(topic)) != null
				|| (buf = remap.get("/" + topic)) != null) {
			ret = buf;
		}
		return ret;
	}

	public static void parseArgs(String[] args) throws URISyntaxException {
		remap.clear();

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
					String[] split = buf.split(":=");
					if (split.length > 1) {
						String key = split[0].trim();
						String val = split[1].trim();
						remap.put(key, val);
						System.out.println("      " + key + " -> " + val);
					}
					mode = 'w';
					break;
				default:
					System.out.println("unknow tag " + buf);
				}
			}
		}

		if (ros_ip == null)
			ros_ip = "127.0.0.1"; // System.getenv("ROS_IP") ;
		if (ros_master == null)
			ros_master = "http://" + ros_ip + ":11311"; // System.getenv("ROS_MASTER_URI")
														// ;
		if (node_name == null)
			node_name = "mouse_controller";

	}

	public static void main(String[] args) {
		try {
			parseArgs(args);
			MouseControlNode mn = new MouseControlNode(node_name, remap);
			NodeConfiguration nodeConfiguration = NodeConfiguration.newPublic(
					ros_ip, new URI(ros_master));
			NodeMainExecutor runner = DefaultNodeMainExecutor.newDefault();
			runner.execute(mn, nodeConfiguration);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
