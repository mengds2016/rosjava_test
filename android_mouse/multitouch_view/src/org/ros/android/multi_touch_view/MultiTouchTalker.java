package org.ros.android.multi_touch_view;


import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Publisher;

public class MultiTouchTalker extends AbstractNodeMain {

	private std_msgs.Float32MultiArray touch_vector;
	private Publisher<std_msgs.Float32MultiArray> touch_vector_publisher;
	private String topic_name = "/multi_touch_view/touch_vector";

	synchronized public void publish(float[] vector) {
		if ( this.touch_vector != null ){
			this.touch_vector.setData(vector) ;
			this.touch_vector_publisher.publish(this.touch_vector) ;
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
	}
}