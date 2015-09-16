import java.net.URI;

import org.ros.node.DefaultNodeMainExecutor;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

public class Main {

	public static void main(String[] args) throws Exception {
		String ros_ip = null, ros_master = null ;
		String portid = null, brate = null ;
		
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
				case 'p':
					System.out.println("port_id " + buf);
					portid = buf;
					break;
				case 'b':
					System.out.println("bond_rate " + buf);
					brate = buf;
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
		if ( portid == null )
			portid = "/dev/ttyUSB0" ;
		if ( brate == null )
			brate = "9600" ;

		NodeConfiguration nodeConfiguration = NodeConfiguration.newPublic(
				ros_ip, new URI(ros_master));
		TakasagoROSInterface qsi = new TakasagoROSInterface(portid, brate);
		NodeMainExecutor runner = DefaultNodeMainExecutor.newDefault();
		runner.execute(qsi, nodeConfiguration);
	}

}
