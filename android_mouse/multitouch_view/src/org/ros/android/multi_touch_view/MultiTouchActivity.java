package org.ros.android.multi_touch_view;

import java.net.InetAddress;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.Display;
import android.view.WindowManager;
import android.widget.TextView;

import org.ros.address.InetAddressFactory;
import org.ros.android.RosDialogActivity;
import org.ros.android.multi_touch_view.R;
import org.ros.exception.RosRuntimeException;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

public class MultiTouchActivity extends RosDialogActivity {

	private MultiTouchView multi_touch_view;
	private TextView bottom_notf ;
	private MultiTouchTalker talker;
	public static float width = 480, height = 640;

	public MultiTouchActivity() {
		super("multi_touch_view", "multi_touch_view");
	}

//	@SuppressWarnings("unchecked")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		talker = new MultiTouchTalker();

		WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
		Display disp = wm.getDefaultDisplay();
		width = disp.getWidth();
		height = disp.getHeight();

		setContentView(R.layout.main);
		multi_touch_view = (MultiTouchView) findViewById(R.id.multi_touch_view);
		multi_touch_view.setTopicName(null);
		multi_touch_view.setTalker(talker) ;
		
		bottom_notf = (TextView) findViewById(R.id.bottom_notification_text) ;
	}

	@Override
	public void onResume() {
		super.onResume();
	}

	@Override
	protected void init(NodeMainExecutor nodeMainExecutor) {
		NodeConfiguration nodeConfiguration = NodeConfiguration.newPublic(
				getHostname(), getMasterUri());
		this.runOnUiThread(new Runnable() {
			public void run() {
				MultiTouchActivity.this.bottom_notf.setText(getHostname()
						+ " --[connect]--> " + getMasterUri());
			}
		});
		nodeMainExecutor.execute(multi_touch_view, nodeConfiguration);
		nodeMainExecutor.execute(talker, nodeConfiguration);
	}
	
}
