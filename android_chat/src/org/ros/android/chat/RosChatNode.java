package org.ros.android.chat;


import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.topic.Publisher;
import org.ros.node.topic.Subscriber;

import android.content.Context;
import android.util.Log;

import std_msgs.Float32MultiArray;

public class RosChatNode extends AbstractNodeMain{

	final private String TAG = "ChatNode" ;
	
	private std_msgs.Float32MultiArray float_msg1, float_msg2;
	private Publisher<std_msgs.Float32MultiArray> touch_vector_publisher;
	private Publisher<std_msgs.Float32MultiArray> pantlt_publisher;
	private Publisher<std_msgs.String> string_publisher;

	
	private String command_connect = RosChatActivity.node_name + "/request/connect";
	private String command_drive = RosChatActivity.node_name + "/request/drive_vector";
	private String command_string = RosChatActivity.node_name + "/request/string";
	private String status_sensors = RosChatActivity.node_name + "/status/sensor_vector";
	private String status_touch = RosChatActivity.node_name + "/status/touch_vector";
	private String status_string = RosChatActivity.node_name + "/status/string";
	
	private RosStringCallback str_callback ;
	private RosFloatVectorCallback fv_callback ;
	
	private Context context ;
	
	public RosChatNode(Context con){
		this.context = con ;
	}
	
	public void setStringCallback(RosStringCallback str_callback){
		this.str_callback = str_callback ;
	}
	
	public void setFloatVectorCallback(RosFloatVectorCallback str_callback){
		this.fv_callback = str_callback ;
	}
	
	synchronized public void touchEventPublish(float[] vector) {
		if ( this.float_msg1 != null ){
			Log.d(TAG, "touch event at " + vector[0] + "/" + vector[1]);
			this.float_msg1.setData(vector) ;
			this.touch_vector_publisher.publish(this.float_msg1) ;
		}
	}
	
	public void publishStringStatus(String str){
		if ( this.string_publisher != null ){
			std_msgs.String msg = this.string_publisher.newMessage();
			msg.setData(str);
			this.string_publisher.publish(msg);
		}
	}
	
	public void publishStatusVector(){
	}
	
//	public void setChat(USB2Roomba roomba){
//		this.roomba = roomba ;
//	}
	
	@Override
	public GraphName getDefaultNodeName() {
		return GraphName.of("chat/chat_node");
	}

	@Override
	public void onStart(final ConnectedNode connectedNode) {
		this.touch_vector_publisher = connectedNode.newPublisher(
				this.status_touch, std_msgs.Float32MultiArray._TYPE);
		this.float_msg1 = connectedNode.getTopicMessageFactory().newFromType(
				std_msgs.Float32MultiArray._TYPE);
		
		this.pantlt_publisher = connectedNode.newPublisher(
				this.status_sensors, std_msgs.Float32MultiArray._TYPE);
		this.float_msg2 = this.pantlt_publisher.newMessage() ;

		this.string_publisher = connectedNode.newPublisher(
				this.status_string, std_msgs.String._TYPE);

		
		Subscriber<std_msgs.Float32MultiArray> subscriber1 = connectedNode
				.newSubscriber( this.command_drive, std_msgs.Float32MultiArray._TYPE) ;
		subscriber1
				.addMessageListener(new MessageListener<std_msgs.Float32MultiArray>() {
					@Override
					public void onNewMessage(Float32MultiArray drive) {
						if ( RosChatNode.this.fv_callback != null ){
							RosChatNode.this.fv_callback.messageArrive( RosChatNode.this.command_drive, drive.getData()) ;
						}
					}
				}, 1) ;
		
		Subscriber<std_msgs.Empty> subscriber3 = connectedNode
				.newSubscriber( this.command_connect, std_msgs.Empty._TYPE) ;
		subscriber3
				.addMessageListener(new MessageListener<std_msgs.Empty>() {
					@Override
					public void onNewMessage(std_msgs.Empty msg) {
					}
				}, 1) ;
		
		Subscriber<std_msgs.String> subscriber4 = connectedNode
				.newSubscriber( this.command_string, std_msgs.String._TYPE) ;
		subscriber4
				.addMessageListener(new MessageListener<std_msgs.String>() {
					@Override
					public void onNewMessage(std_msgs.String msg) {
						if ( RosChatNode.this.str_callback != null ){
							RosChatNode.this.str_callback.messageArrive( RosChatNode.this.command_string, msg.getData()) ;
						}
					}
				}, 1) ;
		
	}
	
	public void onDestroy(){
	}
	
	@Override
	public void onShutdown(Node node) {
		super.onShutdownComplete(node) ;
		this.onDestroy();
	}

	public interface RosStringCallback{
		public void messageArrive(String topic, String msg);
	}
	public interface RosFloatVectorCallback{
		public void messageArrive(String topic, float[] msg);
	}
}