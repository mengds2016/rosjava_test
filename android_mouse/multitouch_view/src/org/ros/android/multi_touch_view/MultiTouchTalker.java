package org.ros.android.multi_touch_view;


import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Publisher;

public class MultiTouchTalker extends AbstractNodeMain {

	private std_msgs.Float32MultiArray touch_vector;
	private Publisher<std_msgs.Float32MultiArray> touch_vector_publisher;
	private String topic_name = "/multi_touch_view/touch_vector";
	private std_msgs.Int32MultiArray event_vector;
	private Publisher<std_msgs.Int32MultiArray> touch_event_publisher;
	private String event_topic_name = "/multi_touch_view/touch_event_vector";
	
	synchronized public void publish(float[] vector) {
		if ( this.touch_vector != null ){
			this.touch_vector.setData(vector) ;
			this.touch_vector_publisher.publish(this.touch_vector) ;
		}
	}
	
	synchronized public void publishEvent(int[] vector) {
		if ( this.event_vector != null ){
			this.event_vector.setData(vector) ;
			this.touch_event_publisher.publish(this.event_vector) ;
		}
	}

	@Override
	public GraphName getDefaultNodeName() {
		return GraphName.of("multi_touch_view/touch_vector_talker");
	}

	@Override
	public void onStart(final ConnectedNode connectedNode) {
		this.touch_vector_publisher = connectedNode.newPublisher(
				this.topic_name, std_msgs.Float32MultiArray._TYPE);
		this.touch_vector = connectedNode.getTopicMessageFactory().newFromType(
				std_msgs.Float32MultiArray._TYPE);
		
//		this.touch_event_publisher = connectedNode.newPublisher(
//				this.event_topic_name, std_msgs.Int32MultiArray._TYPE);
//		this.event_vector = connectedNode.getTopicMessageFactory().newFromType(
//				std_msgs.Int32MultiArray._TYPE);
	}
}