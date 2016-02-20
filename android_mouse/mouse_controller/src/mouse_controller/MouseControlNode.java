package mouse_controller;

import java.awt.AWTException;
import java.awt.MouseInfo;
import java.awt.PointerInfo;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.util.HashMap;

import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Subscriber;

public class MouseControlNode extends AbstractNodeMain implements Runnable {

	protected String nodeName = "mouse_control_node";
	protected ConnectedNode connectedNode;

	private Robot robot;
	private boolean release;
	private float x0, y0;
	private int x, y;

	private Thread thr;
	private long last_mouse_time = 0;
	private long last_down_time = 0;
	private long last_up_time = 0;

	private final static int DN = 0, MV = 1, UP = 2, TP = 3, NG = 4;
	
	public MouseControlNode() {
		this("sensor_image_node");
	}

	public MouseControlNode(String nodeName) {
		this(nodeName, null);
	}

	public MouseControlNode(String nodeName,
			HashMap<String, String> name_space_array) {
		super();
		this.nodeName = nodeName;
		this.release = true;
		try {
			this.robot = new Robot();
		} catch (AWTException e) {
			e.printStackTrace();
		}
		(this.thr = new Thread(this)).start();
	}

	@Override
	public GraphName getDefaultNodeName() {
		return GraphName.of(this.nodeName);
	}

	@Override
	public void finalize() {
		this.thr = null;
	}

	@Override
	public void onStart(ConnectedNode connectedNode) {
		Subscriber<std_msgs.Float32MultiArray> subscriber = connectedNode
				.newSubscriber("/multi_touch_view/touch_vector",
						std_msgs.Float32MultiArray._TYPE);
		subscriber
				.addMessageListener(new MessageListener<std_msgs.Float32MultiArray>() {
					@Override
					public void onNewMessage(std_msgs.Float32MultiArray message) {
						float[] val = message.getData();
						MouseControlNode.this.mouseEventProc(val);
					}
				});
	}

	public void mouseEventProc(float[] val) {
		this.last_mouse_time = System.currentTimeMillis();
		int event = (int)val[3];
		if (this.release || event == MouseControlNode.DN ) {
			PointerInfo pi = MouseInfo.getPointerInfo();
			java.awt.Point pt = pi.getLocation();
			this.x = pt.x;
			this.y = pt.y;
			this.x0 = val[0];
			this.y0 = val[1];
			this.robot.mouseMove(this.x, this.y);
			//
			if ( this.last_mouse_time - this.last_down_time < 100 ){
				this.robot.mousePress(InputEvent.BUTTON1_MASK);
			}
			this.last_down_time = this.last_mouse_time;
			this.release = false;
		} else {
			this.x += val[0] - this.x0;
			this.y += val[1] - this.y0;
			this.x0 = val[0];
			this.y0 = val[1];
			this.robot.mouseMove(this.x, this.y);
		}
		if ( event == MouseControlNode.UP || event == MouseControlNode.TP ){
			this.release = true;
			this.last_up_time = this.last_mouse_time;
			if ( event == MouseControlNode.TP ) this.last_down_time = this.last_mouse_time;
			this.robot.mouseRelease(InputEvent.BUTTON1_MASK);
		}
	}

	@Override
	public void onShutdown(org.ros.node.Node node) {
		super.onShutdown(node);
		try {
			finalize();
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		while (this.thr != null) {
			try {
				Thread.sleep(10);
				if (System.currentTimeMillis() - this.last_mouse_time >= 100) {
					this.robot.mouseRelease(InputEvent.BUTTON1_MASK);
					this.last_up_time = this.last_mouse_time;
					this.release = true;
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

}