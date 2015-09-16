import java.io.IOException;

import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.topic.Publisher;

public class TakasagoROSInterface extends AbstractNodeMain implements Runnable {

	private String portid ;
	private int brate ;
	
	private Publisher<std_msgs.Float64> current_publisher;
	private Publisher<std_msgs.Float64> voltage_publisher;
	private Publisher<std_msgs.String> feedback_publisher;
	private TakasagoInterface ti;

	final public static String nodename = "takasago_interface";

	public TakasagoROSInterface(String portid, String brate) {
		this.portid = portid ;
		this.brate = Integer.parseInt(brate) ;
		this.ti = new TakasagoInterface(this.portid, this.brate);
	}

	@Override
	public GraphName getDefaultNodeName() {
		return GraphName.of(TakasagoROSInterface.nodename);
	}

	@Override
	public void onStart(final ConnectedNode connectedNode) {
		this.current_publisher = connectedNode.newPublisher(
				TakasagoROSInterface.nodename + "/current",
				std_msgs.Float64._TYPE);
		this.voltage_publisher = connectedNode.newPublisher(
				TakasagoROSInterface.nodename + "/voltage",
				std_msgs.Float64._TYPE);
		this.feedback_publisher = connectedNode.newPublisher(
				TakasagoROSInterface.nodename + "/feedback",
				std_msgs.String._TYPE);

		new Thread(this).start();
	}

	@Override
	public void onShutdown(Node node) {
		try {
			this.ti._finalize();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void finalize() throws Throwable {
		try {
			super.finalize();
			this.ti._finalize();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
		}
	}

	@Override
	public void run() {
		try {
			this.feedback_publish(" start takasago-interface thread");
			this.ti.write("address 1");
			if ( this.ti.readLine().contentEquals("ERROR") ){
				this.feedback_publish("connection lost") ;
			}
			String command="";
			while (true) {
				try{
					this.ti.write("measure:curr?");
					Thread.sleep(50);
					command = this.ti.readLine();
					this.current_publish(Double.parseDouble(command));
					Thread.sleep(50);
					//
					this.ti.write("measure:volt?");
					command = this.ti.readLine();
					Thread.sleep(50);
					this.voltage_publish(Double.parseDouble(command));
					Thread.sleep(50);
				} catch (NumberFormatException e) {
					this.feedback_publish("ERROR: " + command) ;
					e.printStackTrace();
					break ;
				} catch (InterruptedException e) {
					this.feedback_publish("SLEEP ERROR") ;
					e.printStackTrace();
					break ;
				} 
				this.feedback_publish("running takasago-interface thread") ;
			}
			this.feedback_publish(" stop takasago-interface thread");
		} catch (IOException e) {
			this.feedback_publish("IO ERROR") ;
			e.printStackTrace();
		}
	}
	
	public void feedback_publish(String str){
		System.out.println("--feedback, " + str) ;
		std_msgs.String ros_str = this.feedback_publisher.newMessage() ;
		ros_str.setData(str) ;
		this.feedback_publisher.publish(ros_str) ;
	}
	
	public void current_publish(double d){
		std_msgs.Float64 ros_f = this.current_publisher.newMessage() ;
		ros_f.setData(d) ;
		this.current_publisher.publish(ros_f) ;
	}

	public void voltage_publish(double d){
		std_msgs.Float64 ros_f = this.voltage_publisher.newMessage() ;
		ros_f.setData(d) ;
		this.voltage_publisher.publish(ros_f) ;
	}

}