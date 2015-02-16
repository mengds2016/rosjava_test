package org.ros.android.chat;

import java.util.ArrayList;
import java.util.List;

import android.app.ProgressDialog;
import android.content.Intent;
import android.hardware.Camera;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import org.ros.android.RosDialogActivity;
import org.ros.android.chat.RobotBarNode.TaggedIcon;
import org.ros.android.chat.RosChatNode.RosFloatVectorCallback;
import org.ros.android.chat.RosChatNode.RosStringCallback;
import org.ros.android.chat.R;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;
import org.ros.node.topic.Subscriber;

public class RobotBarActivity extends RosDialogActivity implements
		SurfaceHolder.Callback, Runnable {

	final private String TAG = "RobotBarActivity";

	private CompressedImageView image_view_left, image_view_right;
	private CompressedImageView image_view_small;
	private TextView bottom_notf;
	private EditText edit_text;

	private RosChatNode chatnode;
	private Camera camera;
	private SurfaceView surf;
	private int camera_width = -1, camera_height = -1;

	private ImagePublishNode image_publisher;
	private AudioPubSubNode audio_node;
	private AndroidPosePubNode pose_node;
	private KubiControlNode kubi_node;
	private TextPubNode text_node;
	public static RobotBarNode rb_node;

	private Button move_to_demo_button;

	private Thread chat_observer;
	private boolean image_publishing;

	private ProgressDialog pDialog;

	private static boolean client_p = true;
	public static String node_name = "kubi_chat";
	static {
		if (client_p)
			node_name = "ros_chat";
	}

	private LinearLayout toggle_button_layout;
	private LinearLayout tagged_demo_button;
	private ArrayList<TaggedIcon> demo_icons;
	
	private boolean rosparam_loading;
	
	public RobotBarActivity() {
		super(node_name, node_name, node_name);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.robot_bar_act);

		this.rosparam_loading = false;

		this.image_publishing = false;

		this.toggle_button_layout = (LinearLayout) findViewById(R.id.toggle_button_outer);
		this.tagged_demo_button = (LinearLayout) findViewById(R.id.taged_image_buttons);
		this.demo_icons = new ArrayList<TaggedIcon>();
		DemoMakeActivity.demo_icons = new ArrayList<TaggedIcon>();

		this.surf = (SurfaceView) findViewById(R.id.camera_surface);
		SurfaceHolder holder = this.surf.getHolder();
		holder.addCallback(this);
		holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

		this.pDialog = new ProgressDialog(this);
		this.pDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		this.pDialog.setIndeterminate(false);
		this.pDialog.setCancelable(true);

		this.move_to_demo_button = (Button) findViewById(R.id.demo_craete_move_button);
		this.move_to_demo_button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				RobotBarActivity.this.pDialog.show();
				new Thread(new Runnable() {
					@Override
					public void run() {
						Intent i = new Intent(RobotBarActivity.this
								.getApplicationContext(),
								DemoMakeActivity.class);
						RobotBarActivity.this.startActivity(i);
						RobotBarActivity.this.pDialog.dismiss();
					}
				}).start();
			}
		});
		
	}

	// for robot_bar_act layout
	public void onClickDemoIcon(View v) {
		if (this.chatnode != null) {
			this.chatnode.publishStringStatus("tag:" + v.getTag());
		}
	}

	public void onClickControlToggle(View v) {
		String msg = "toggle:";
		try {
			for (int i = 0; i < this.toggle_button_layout.getChildCount(); i++) {
				ToggleButton tb = (ToggleButton) this.toggle_button_layout
						.getChildAt(i);
				if (tb.isChecked()) {
					msg += "o";
				} else {
					msg += "x";
				}
			}
		} catch (Exception e) {
		}
		this.chatnode.publishStringStatus(msg);
	}

	public void initializeNodes() {
		node_name = getNodename();
		this.chatnode = new RosChatNode(this.getApplicationContext());

		this.image_view_left = (CompressedImageView) findViewById(R.id.compressed_image_view_left);
		this.image_view_left.setNodeName(node_name + "/image_view_left");
		this.image_view_left.setTopicName(node_name + "/request/"
				+ "image_left/raw/compressed");
		this.image_view_left.setMessageType(sensor_msgs.CompressedImage._TYPE);
		this.image_view_left.setTalker(this.chatnode);

		this.image_view_right = (CompressedImageView) findViewById(R.id.compressed_image_view_right);
		this.image_view_right.setNodeName(node_name + "/image_view_right");
		this.image_view_right.setTopicName(node_name + "/request/"
				+ "image_right/raw/compressed");
		this.image_view_right.setMessageType(sensor_msgs.CompressedImage._TYPE);
		this.image_view_right.setTalker(this.chatnode);

		this.image_view_small = (CompressedImageView) findViewById(R.id.compressed_image_view_small);
		this.image_view_small.setTopicName(node_name + "/request/"
				+ "image/raw/small/compressed");
		this.image_view_small.setMessageType(sensor_msgs.CompressedImage._TYPE);
		this.image_view_small.setNodeName(node_name
				+ "/compressed_image_view_small");

		this.audio_node = new AudioPubSubNode(node_name);

		this.bottom_notf = (TextView) findViewById(R.id.bottom_notification_text);

		this.edit_text = (EditText) findViewById(R.id.editabletextbox);

		// this.connect() ;

		this.chat_observer = new Thread(this);
		this.chat_observer.start();

		this.image_view_left.setChat(this.chatnode);
		this.image_view_right.setChat(this.chatnode);

		this.image_publisher = new ImagePublishNode();

		this.chatnode.setStringCallback(new RosStringCallback() {
			@Override
			public void messageArrive(String topic, String msg) {
				final String tp = topic;
				final String mg = msg;
				Log.d(TAG, "[Message arrive] " + mg + " at " + tp);
				RobotBarActivity.this.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						RobotBarActivity.this.bottom_notf
								.setText("[Message arrive] " + mg + " at " + tp);
					}
				});
			}
		});
		this.chatnode.setFloatVectorCallback(new RosFloatVectorCallback() {
			@Override
			public void messageArrive(String topic, float[] msg) {
				final String tp = topic;
				final String mg = msg[0] + " " + msg[1];
				Log.d(TAG, "[Message arrive] " + mg + " at " + tp);
				RobotBarActivity.this.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						RobotBarActivity.this.bottom_notf
								.setText("[Message arrive] " + mg + " at " + tp);
					}
				});
			}
		});

		this.pose_node = new AndroidPosePubNode(node_name,
				(SensorManager) getSystemService(SENSOR_SERVICE));
		this.pose_node.onResume();

		this.text_node = new TextPubNode(this.nodename_org, this.edit_text);

		if (!client_p) {
			this.kubi_node = new KubiControlNode(this, node_name);
		}

		rb_node = new RobotBarNode(this);
	}

	@Override
	protected void init(NodeMainExecutor nodeMainExecutor) {

		runOnUiThread(new Runnable(){
			@Override
			public void run(){
				RobotBarActivity.this.pDialog.show();
				Toast.makeText(RobotBarActivity.this, "connecting...", Toast.LENGTH_LONG).show();
			}});

		initializeNodes();

		NodeConfiguration nodeConfiguration = NodeConfiguration.newPublic(
				getHostname(), getMasterUri());
		this.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				RobotBarActivity.this.bottom_notf
						.setText(RobotBarActivity.this.bottom_notf.getText()
								+ "/ROS: " + getHostname() + " --[connect]--> "
								+ getMasterUri());
			}
		});
		nodeMainExecutor.execute(this.image_view_left, nodeConfiguration);
		nodeMainExecutor.execute(this.image_view_right, nodeConfiguration);
		nodeMainExecutor.execute(this.image_view_small, nodeConfiguration);
		nodeMainExecutor.execute(this.image_publisher, nodeConfiguration);
		nodeMainExecutor.execute(this.chatnode, nodeConfiguration);
		nodeMainExecutor.execute(this.audio_node, nodeConfiguration);
		nodeMainExecutor.execute(this.pose_node, nodeConfiguration);
		nodeMainExecutor.execute(this.text_node, nodeConfiguration);
		if (this.kubi_node != null)
			nodeMainExecutor.execute(this.kubi_node, nodeConfiguration);
		nodeMainExecutor.execute(rb_node, nodeConfiguration);

		// vibration
		nodeMainExecutor.execute(new AbstractNodeMain() {
			private Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

			@Override
			public GraphName getDefaultNodeName() {
				return GraphName.of(RobotBarActivity.node_name
						+ "/vibrate_node");
			}

			@Override
			public void onStart(final ConnectedNode connectedNode) {
				Subscriber<std_msgs.Int64> subscriber1 = connectedNode
						.newSubscriber(RobotBarActivity.node_name
								+ "/request/vibrate", std_msgs.Int64._TYPE);
				subscriber1.addMessageListener(
						new MessageListener<std_msgs.Int64>() {
							@Override
							public void onNewMessage(std_msgs.Int64 tm) {
								vibrator.vibrate(tm.getData());
							}
						}, 1);
			}
		}, nodeConfiguration);

		if (this.camera != null && !this.image_publishing) {
			this.image_publishing = true;
			Camera.Parameters param = this.camera.getParameters();
			this.image_publisher
					.startImagePublisher(this.camera,
							param.getPreviewSize().width,
							param.getPreviewSize().height);
		}

		runOnUiThread(new Runnable(){
			@Override
			public void run(){
				RobotBarActivity.this.pDialog.dismiss();
				Toast.makeText(RobotBarActivity.this, "connected!", Toast.LENGTH_LONG).show();
			}});
	}

	// private void openCamera(SurfaceHolder holder) throws IOException {
	// if (myCamera == null) {
	// // myCamera = Camera.open();
	// // if (myCamera == null) {
	// // myCamera = Camera.open(0);
	// // }
	// try {
	// myCamera = Camera.open(this.cameraId);
	// } catch (NoSuchMethodError e) {
	// e.printStackTrace();
	// myCamera = Camera.open();
	// }
	// if (myCamera == null) {
	// throw new IOException();
	// }
	// }
	// myCamera.setPreviewDisplay(holder);
	// // if (gl == null) {
	// myCamera.setOneShotPreviewCallback(this);
	// myCamera.startPreview();
	// // }
	// // myCamera.setOneShotPreviewCallback(this) ;
	// }

	private void setupCamera() {
		int cameraId = 0;
		Camera.CameraInfo info = new Camera.CameraInfo();
		for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
			Camera.getCameraInfo(i, info);
			if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
				cameraId = i;
			}
		}
		//
		try {
			if (this.camera == null) {
				this.camera = Camera.open(cameraId);
			}
		} catch (Exception e) {
			e.printStackTrace();
			this.camera = null;
			return;
		}
		if (this.camera_height < 0 || this.camera_width < 0) {
			int width, height;
			width = height = 300;
			double min = 1e+30;
			double target_size = width * height;

			Camera.Parameters param = this.camera.getParameters();
			List<Camera.Size> sizes = param.getSupportedPictureSizes();
			for (Camera.Size size : sizes) {
				Log.d(TAG, "camera size = " + size.width + "x" + size.height);
				if (Math.abs(target_size - size.width * size.height) < min) {
					try {
						Log.d(TAG, "set camera size = " + size.width + "x"
								+ size.height);
						param.setPreviewSize(size.width, size.height);
						this.camera.setParameters(param);
						min = Math.abs(target_size - size.width * size.height);
						width = size.width;
						height = size.height;
					} catch (Exception e) {
						Log.d(TAG, "?? unsupported " + size.width + "x"
								+ size.height);
					}
				}
			}
			this.camera_width = width;
			this.camera_height = height;

			// this.runOnUiThread(new Runnable() {
			// @Override
			// public void run() {
			// float view_rate = (float) Math.min(
			// 300.0 / ChatActivity.this.camera_width,
			// 300.0 / ChatActivity.this.camera_height);
			// // ViewGroup.LayoutParams view_param =
			// // this.surf.getLayoutParams();
			// FrameLayout.LayoutParams view_param = new
			// FrameLayout.LayoutParams(
			// 300, 300);
			// view_param.width = (int) (view_rate *
			// ChatActivity.this.camera_width);
			// view_param.height = (int) (view_rate *
			// ChatActivity.this.camera_height);
			// ChatActivity.this.surf.setLayoutParams(view_param);
			// }
			// });
		}

		int rotation = getWindowManager().getDefaultDisplay().getRotation();
		int degrees = 0;
		switch (rotation) {
		case Surface.ROTATION_0:
			degrees = 0;
			break;
		case Surface.ROTATION_90:
			degrees = 1;
			break;
		case Surface.ROTATION_180:
			degrees = 2;
			break;
		case Surface.ROTATION_270:
			degrees = 3;
			break;
		}
		// this.image_publisher.setRotateCnt(degrees);
		degrees = degrees * 90;
		int result;
		if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
			result = (info.orientation + degrees) % 360;
			result = (360 - result) % 360;
		} else {
			result = (info.orientation - degrees + 360) % 360;
		}
		this.camera.setDisplayOrientation(result);
		// this.camera.startPreview();
	}

	// private long last_connect_trial ;
	// public void connect(){
	// Log.e("chatActivity"," try to connect") ;
	// if ( ! this.isDestroyed() && ! this.isFinishing() &&
	// (this.last_connect_trial + 3000 < System.currentTimeMillis())){
	// this.last_connect_trial = System.currentTimeMillis() ;
	// }
	// }

	public void updateDemoIcons() {
		if (rb_node != null && !this.rosparam_loading) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					RobotBarActivity.this.rosparam_loading = true;
					int cnt = rb_node
							.getNewDemos(RobotBarActivity.this.demo_icons);
					if ( cnt < 0 ){
						RobotBarActivity.this.runOnUiThread(
								new Runnable(){
									@Override
									public void run(){
										Toast.makeText(RobotBarActivity.this, "error: server missing", Toast.LENGTH_LONG).show();
									}});
					}
					for (int i = 0; i < cnt; i++) {
						TaggedIcon ic = RobotBarActivity.this.demo_icons.get(i);
						if (ic.icon != null) {
							final ImageButton imageButton = new ImageButton(
									RobotBarActivity.this);
							imageButton.setScaleType(ScaleType.FIT_XY);
							imageButton.setAdjustViewBounds(true);
							imageButton.setImageBitmap(ic.icon);
							imageButton.setTag(ic.tag);
							imageButton
									.setOnClickListener(new OnClickListener() {
										@Override
										public void onClick(View v) {
											RobotBarActivity.this.chatnode
													.publishStringStatus("tag:"
															+ v.getTag());
										}
									});
							RobotBarActivity.this.runOnUiThread(new Runnable() {
								@Override
								public void run() {
									RobotBarActivity.this.tagged_demo_button
											.addView(imageButton);
								}
							});
						} else {
							final Button bt = new Button(RobotBarActivity.this);
							bt.setTag(ic.tag);
							bt.setText(ic.name);
							bt.setOnClickListener(new OnClickListener() {
								@Override
								public void onClick(View v) {
									RobotBarActivity.this.chatnode
											.publishStringStatus("tag:"
													+ v.getTag());
								}
							});
							RobotBarActivity.this.runOnUiThread(new Runnable() {
								@Override
								public void run() {
									RobotBarActivity.this.tagged_demo_button
											.addView(bt);
								}
							});
						}
					}
//					if ( RobotBarActivity.this.pDialog.isShowing() ){
//						RobotBarActivity.this.pDialog.dismiss();
//					}
					RobotBarActivity.this.rosparam_loading = false;
				}
			}).start();
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		if (this.pose_node != null) {
			this.pose_node.onResume();
		}
		updateDemoIcons();
	}

	@Override
	public void onPause() {
		super.onPause();
		if (this.pose_node != null) {
			this.pose_node.onPause();
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (this.camera != null) {
			this.image_publisher.stopImagePublisher();
			this.camera.stopPreview();
			this.camera.release();
			this.camera = null;
		}
		this.chatnode.onDestroy();
		this.chat_observer = null;
		this.audio_node.onDestroy();
		if (this.kubi_node != null)
			this.kubi_node.onDestroy();
		if (rb_node != null) {
			rb_node.onDestroy();
			rb_node = null;
		}
	}

	@Override
	public void finalize() throws Throwable {
		onDestroy();
		super.finalize();
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		System.out.println("-- RosChatActiivty surfaceChanged called");
		setupCamera();
		try {
			if (this.camera != null) {
				this.camera.setPreviewDisplay(holder);
				this.camera.startPreview();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (!this.image_publishing && this.camera != null
				&& this.image_publisher != null) {
			this.image_publishing = true;
			Camera.Parameters param = this.camera.getParameters();
			this.image_publisher
					.startImagePublisher(this.camera,
							param.getPreviewSize().width,
							param.getPreviewSize().height);
		}
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		System.out.println("-- RosChatActiivty surfaceCreated called");
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		System.out.println("-- RosChatActiivty surfaceDestroyed called");
		if (this.camera != null && this.image_publishing) {
			this.image_publishing = false;
			this.image_publisher.stopImagePublisher();
			this.camera.stopPreview();
			this.camera.release();
			this.camera = null;
		}
	}

	@Override
	public void run() {
		while (this.chat_observer != null) {
			try {
				Thread.sleep(100);
				if (this.pose_node != null) {
					// this.pose_node.pubTwist();
					this.pose_node.pubPose();
					Thread.sleep(100);
				}
				if (this.kubi_node != null) {
					this.kubi_node.pantltPublish();
					Thread.sleep(100);
				}
				if (this.chatnode != null) {
					onClickControlToggle(null);
					Thread.sleep(100);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		}
	}

}
