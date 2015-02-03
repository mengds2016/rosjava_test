package org.ros.android.chat;

import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Publisher;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class AndroidPosePubNode extends AbstractNodeMain implements SensorEventListener{

	private String nodename = "android_pose";
	
	private Publisher<geometry_msgs.TwistStamped> twist ;
	private geometry_msgs.TwistStamped tw ;
	
	private Publisher<geometry_msgs.PoseStamped> pose ;
	private geometry_msgs.PoseStamped ps ;
	
	private SensorManager mSensorManager;
    private Sensor mAccelerometer, mOrientation;
    private float[] p, v, q, r, rpy ;
    
    private boolean listener_registered = false;
	
	public AndroidPosePubNode(String nodeName, SensorManager manager){
		super() ;
		this.nodename = nodeName;
		this.v = new float[3] ;
		this.p = new float[3] ;
		this.q = new float[4] ;
		this.r = new float[9] ;
		this.rpy = new float[3] ;
		this.mSensorManager = manager;
		this.mAccelerometer = this.mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
		this.mOrientation = this.mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
	}

	@Override
	public GraphName getDefaultNodeName() {
		return GraphName.of(nodename);
	}

	@Override
	public void onStart(final ConnectedNode connectedNode) {
		this.twist = connectedNode.newPublisher(nodename + "/twistStamped",
				geometry_msgs.TwistStamped._TYPE);
		this.tw = this.twist.newMessage() ;
		this.pose = connectedNode.newPublisher(nodename + "/poseStamped",
				geometry_msgs.PoseStamped._TYPE);
		this.ps = this.pose.newMessage() ;
	}
	
	protected void onResume() {
		if ( ! this.listener_registered ){
			this.listener_registered = true;
			this.mSensorManager.registerListener(this, this.mOrientation,
					SensorManager.SENSOR_DELAY_NORMAL);
			this.mSensorManager.registerListener(this, this.mAccelerometer,
					SensorManager.SENSOR_DELAY_FASTEST);
		}
	}

	protected void onPause() {
		if ( this.listener_registered ){
			this.listener_registered = false;
			this.mSensorManager.unregisterListener(this);
		}
	}
	
	public void pubTwist(float[] xyz, float[] rpy){
		if (this.tw != null && xyz != null && rpy != null) {
			this.tw.getHeader().setFrameId("/map") ;
			//this.tw.getHeader().setStamp( new Time(0) ) ;
			this.tw.getHeader().setSeq((int)(System.currentTimeMillis()/1000)) ;
			this.tw.getTwist().getLinear().setX(xyz[0]);
			this.tw.getTwist().getLinear().setY(xyz[1]);
			this.tw.getTwist().getLinear().setZ(xyz[2]);
			this.tw.getTwist().getAngular().setX(rpy[0]);
			this.tw.getTwist().getAngular().setY(rpy[1]);
			this.tw.getTwist().getAngular().setZ(rpy[2]);
			this.twist.publish(this.tw);
		}
	}
	
	public void pubPose(){
		pubPose(this.p, this.q);
	}
	
	public void pubPose(float[] xyz, float[] q){
		if (this.ps != null && xyz != null && q != null) {
			this.ps.getHeader().setFrameId("/map") ;
			//this.ps.getHeader().setStamp( new Time(0) ) ;
			this.ps.getHeader().setSeq((int)(System.currentTimeMillis()/1000)) ;
			this.ps.getPose().getPosition().setX(xyz[0]);
			this.ps.getPose().getPosition().setY(xyz[1]);
			this.ps.getPose().getPosition().setZ(xyz[2]);
			this.ps.getPose().getOrientation().setW(q[0]);
			this.ps.getPose().getOrientation().setX(q[1]);
			this.ps.getPose().getOrientation().setY(q[2]);
			this.ps.getPose().getOrientation().setZ(q[3]);
			this.pose.publish(this.ps) ;
		}
	}
	
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}
	
	public void quaternion2rpy( float[] rpy, float[] q ){ // w x y z
		float w = q[0] ;
		double xyz_s = Math.sqrt(q[1]*q[1] + q[2]*q[2] + q[3]*q[3]) ;
		double thre = 2.0 * Math.atan2(xyz_s, w) ;
		if ( thre > Math.PI ){
			thre = thre - 2*Math.PI ;
		} else if ( thre < - Math.PI ){
			thre = thre + 2*Math.PI ;
		}
		thre = thre / xyz_s ;
		// z = toward the sky, y = toward the north pole
		rpy[0] = (float)( q[1] * thre ) ;
		rpy[1] = (float)( q[2] * thre ) ;
		rpy[2] = (float)( q[3] * thre ) ;
	}

	private long last_update_acc = -1;
	private float[] acc_zero = new float[3] ;
	@Override
	public void onSensorChanged(SensorEvent event) {
		switch(event.sensor.getType()){
        case Sensor.TYPE_LINEAR_ACCELERATION:
           	if ( this.last_update_acc < 0 ){
        		this.last_update_acc = System.currentTimeMillis() ;
        		for ( int i=0 ; i<3 ; i++ ){
        			this.acc_zero[i] = event.values[i] ;
        		}
        	}
			float[] acc = new float[3];
			for (int i = 0; i < 3; i++) {
				for (int j = 0; j < 3; j++) {
					acc[i] += this.r[j + i * 3] * event.values[j] ;
				}
				acc[i] -= this.acc_zero[i] ;
			}
			if ( Math.sqrt(acc[0]*acc[0] + acc[1]*acc[1] + acc[2]*acc[2]) < 0.15 ){
				for ( int i=0 ; i<3 ; i++ ){
				//	System.out.print(acc[i] + " ") ;
				//	this.acc_zero[i] += 0.1 * acc[i] ;
					this.acc_zero[i] = acc[i] ;
					this.v[i] *= 0.7 ;
				}
				//System.out.println( " <-- skip" ) ;
			} else {
	        	double step = 1e-3 * (System.currentTimeMillis() - this.last_update_acc) ;
	        	int[] pm = new int[]{-1, 1, 1} ;
	        	for ( int i=0 ; i<3 ; i++ ){
	        		this.v[i] += 0.7 * step * acc[i] ;
	        		this.p[i] += 0.7 * pm[i] * step * this.v[i] ;
				}
			}
        	this.last_update_acc = System.currentTimeMillis() ;
            break;
        case Sensor.TYPE_ROTATION_VECTOR:
            SensorManager.getQuaternionFromVector(this.q, event.values);
            SensorManager.getRotationMatrixFromVector(this.r,event.values) ;   
            this.quaternion2rpy(this.rpy, this.q);
 
        	break;
        default:
        	System.out.print( "unknow:" + event.sensor.getType() + " "  ) ;
        }
		
	}

}
