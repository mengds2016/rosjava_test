package org.ros.android.chat;


import java.util.ArrayList;

import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.topic.Publisher;
import org.ros.node.topic.Subscriber;

import android.content.Context;
import android.util.Log;

import com.revolverobotics.kubiapi.IKubiManagerDelegate;
import com.revolverobotics.kubiapi.Kubi;
import com.revolverobotics.kubiapi.KubiManager;
import com.revolverobotics.kubiapi.KubiSearchResult;

import std_msgs.Float32MultiArray;

public class KubiControlNode extends AbstractNodeMain implements Runnable, IKubiManagerDelegate{

	private final String TAG = "KubiControlNode";
	
	private String node_name = "ros_chat";
	
	private std_msgs.Float32MultiArray float_msg1, float_msg2;
	private Publisher<std_msgs.Float32MultiArray> pantlt_publisher;

	private String command_connect = node_name + "/connect_request";
	private String command_pantlt = node_name + "/request/pan_tilt_vector";
	private String command_pantlt_relative = node_name + "/request/pan_tilt_vector/relative";
	private String status_pantlt = node_name + "/status/pan_tilt_vector";
	
	private KubiManager kubi_manager ;
	
	private Thread state_publish_thread ;
	private Context context;

	public KubiControlNode(Context con, String node_name){
		this.context = con ;
		this.node_name = node_name;
		this.kubi_manager = new KubiManager(this,false) ;
		kubi_connect();
	}

	public void pantltPublish(){
		if ( this.kubi_manager != null && this.kubi_manager.getKubi() != null ){
			Kubi kubi = this.kubi_manager.getKubi();
			float pan = kubi.getPan() ;
			float tlt = kubi.getTilt() ;
			Log.d("kubiNode", "status publish " + pan + "/" + tlt) ;
			this.float_msg2.setData( new float[]{ pan, tlt } ) ;
			this.pantlt_publisher.publish(this.float_msg2) ;
		}
	}
	
	private long last_connect_trial = 0 ;
	public boolean kubi_connect(){
		Log.e(TAG," try to connect") ;
		if (this.last_connect_trial + 3000 < System.currentTimeMillis()){
			this.last_connect_trial = System.currentTimeMillis() ;
			this.kubi_manager.findAllKubis() ;
		}
		return true;
	}
	
	public boolean kubiMove(float pan, float tlt, boolean relative){
		if (this.kubi_manager != null
				&& this.kubi_manager.getKubi() != null ) {
			Kubi kubi = this.kubi_manager.getKubi() ;
			if ( relative ){
				pan = kubi.getPan() + pan ;
				tlt = kubi.getTilt() + tlt ;
			}
			if ( pan >= 90 ) pan = 90 - 1f ;
			if ( tlt >= 90 ) tlt = 90 - 1f ;
			if ( pan <= -90 ) pan = -90 + 1f ;
			if ( tlt <= -90 ) tlt = -90 + 1f ;
			Log.d("kubiNode", "move " + pan + "," + tlt) ;
			kubi.moveTo(pan, tlt ) ;
			return true;
		} else {
			return kubi_connect();
		}
	}

	@Override
	public GraphName getDefaultNodeName() {
		return GraphName.of(node_name + "/kubi_controller");
	}

	@Override
	public void onStart(final ConnectedNode connectedNode) {
		this.float_msg1 = connectedNode.getTopicMessageFactory().newFromType(
				std_msgs.Float32MultiArray._TYPE);
		
		this.pantlt_publisher = connectedNode.newPublisher(
				this.status_pantlt, std_msgs.Float32MultiArray._TYPE);
		this.float_msg2 = this.pantlt_publisher.newMessage() ;
		
		Subscriber<std_msgs.Float32MultiArray> subscriber1 = connectedNode
				.newSubscriber( this.command_pantlt, std_msgs.Float32MultiArray._TYPE) ;
		subscriber1
				.addMessageListener(new MessageListener<std_msgs.Float32MultiArray>() {
					@Override
					public void onNewMessage(Float32MultiArray pan_tilt) {
						if (pan_tilt.getData().length >= 2) {
							KubiControlNode.this.kubiMove(pan_tilt.getData()[0], pan_tilt.getData()[1], false) ;
						}
					}
				}, 1) ;
		
		Subscriber<std_msgs.Float32MultiArray> subscriber2 = connectedNode
				.newSubscriber( this.command_pantlt_relative, std_msgs.Float32MultiArray._TYPE) ;
		subscriber2
				.addMessageListener(new MessageListener<std_msgs.Float32MultiArray>() {
					@Override
					public void onNewMessage(Float32MultiArray pan_tilt) {
						if (pan_tilt.getData().length >= 2) {
							KubiControlNode.this.kubiMove(pan_tilt.getData()[0], pan_tilt.getData()[1], true) ;
						}
					}
				}, 1) ;
		
		Subscriber<std_msgs.Empty> subscriber3 = connectedNode
				.newSubscriber( this.command_connect, std_msgs.Empty._TYPE) ;
		subscriber3
				.addMessageListener(new MessageListener<std_msgs.Empty>() {
					@Override
					public void onNewMessage(std_msgs.Empty msg) {
						kubi_connect() ;
					}
				}, 1) ;
		
		//this.state_publish_thread = new Thread(this) ;
		//this.state_publish_thread.start() ;
	}
	
	@Override
	public void finalize() throws Throwable{
		if ( this.kubi_manager != null && this.kubi_manager.getKubi() != null ){
			this.kubi_manager.getKubi().disconnect() ;
		}
		this.state_publish_thread = null ;
		super.finalize();
	}
	
	public void onDestroy(){
		try {
			finalize();
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void onShutdown(Node node) {
		super.onShutdownComplete(node) ;
		onDestroy();
	}

	@Override
	// no use
	public void run() {
		while ( this.state_publish_thread != null ){
			try {
				Thread.sleep(1000) ;
				this.pantltPublish() ;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	// for kubi
	@Override
	public void kubiDeviceFound(KubiManager arg0, KubiSearchResult arg1) {
		Log.e(TAG,"kubiDeviceFound") ;
		this.kubi_manager.connectToKubi(arg1) ;
	}

	@Override
	public void kubiManagerFailed(KubiManager arg0, int arg1) {
		Log.e(TAG,"kubiManagerFailed[reason=" + arg1 + "]") ;
		this.kubi_connect();
	}

	@Override
	public void kubiManagerStatusChanged(KubiManager arg0, int arg1, int arg2) {
		Log.e(TAG,"kubiManagerStatusChanged " + arg1 + "->" + arg2) ;
		if ( arg2 == KubiManager.STATUS_CONNECTED ){
			Log.e("kubiActivity","kubiManagerStatusChanged connected") ;
			final Kubi kubi = this.kubi_manager.getKubi();
			if ( kubi != null ){
				Log.d("kubiActivity"," connect to " + kubi.getKubiID()) ;
			}
		} else if ( arg2 == KubiManager.STATUS_DISCONNECTED ){
			Log.e("kubiActivity","kubiManagerStatusChanged disconnected") ;
			this.kubi_connect() ;
		}
	}

	@Override
	public void kubiScanComplete(KubiManager arg0,
			ArrayList<KubiSearchResult> arg1) {
		Log.e(TAG,"kubiScanComplete[" + arg1.size() + "devices]") ;
		boolean connect = true ;
		for ( KubiSearchResult r : arg1 ){
			Log.e("kubiActivity",r.getName()) ;
			//if ( this.kubi == null && connect ){
			if ( connect ){
				connect = false ;
				this.kubi_manager.connectToKubi(r) ;
			}
		}
	}

}