package org.ros.android.chat;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ImageView.ScaleType;

import org.ros.android.chat.R;
import org.ros.android.chat.RobotBarNode.TaggedIcon;

public class DemoMakeActivity extends Activity {

	final private String TAG = "DemoMakeActivity";

	private EditText edit_text;
	private ImageButton back_button;
	private Button move_to_pose_button;

	private LinearLayout tagged_demo_button;
//	private LinearLayout selected_motion_layout;
	private View selected_motion_view;
	private ArrayList<TaggedIcon> demo_icons;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.demo_make_act);

		this.demo_icons = new ArrayList<TaggedIcon>();
		this.tagged_demo_button = (LinearLayout) this
				.findViewById(R.id.taged_pose_image_buttons);
//		this.selected_motion_layout = (LinearLayout) this
//				.findViewById(R.id.selected_pose_image_view);
		this.selected_motion_view = this.tagged_demo_button.getChildAt(0);
		this.selected_motion_view.setBackgroundColor(Color.GREEN);

		this.back_button = (ImageButton) findViewById(R.id.demo_craete_back_to_home_button);
		this.back_button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				DemoMakeActivity.this.finish();
			}
		});

		this.move_to_pose_button = (Button) findViewById(R.id.pose_craete_move_button);
		this.move_to_pose_button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent i = new Intent(DemoMakeActivity.this
						.getApplicationContext(), PoseMakeActivity.class);
				DemoMakeActivity.this.startActivity(i);
			}
		});
	}

	@Override
	public void onResume() {
		super.onResume();

		if (RobotBarActivity.rb_node != null) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					int cnt = RobotBarActivity.rb_node
							.getNewDemos(DemoMakeActivity.this.demo_icons);
					for (int i = 0; i < cnt; i++) {
						TaggedIcon ic = DemoMakeActivity.this.demo_icons.get(i);
						if (ic.icon != null) {
							final ImageButton imageButton = new ImageButton(
									DemoMakeActivity.this);
							imageButton.setScaleType(ScaleType.FIT_XY);
							imageButton.setAdjustViewBounds(true);
							imageButton.setImageBitmap(ic.icon);
							imageButton.setTag(ic.tag);
							imageButton
									.setOnClickListener(new OnClickListener() {
										@Override
										public void onClick(View v) {
											onClickMotionIcon(v);
										}
									});
							DemoMakeActivity.this.runOnUiThread(new Runnable() {
								@Override
								public void run() {
									DemoMakeActivity.this.tagged_demo_button
											.addView(imageButton);
								}
							});
						} else {
							final Button bt = new Button(DemoMakeActivity.this);
							bt.setTag(ic.tag);
							bt.setText(ic.tag);
							bt.setOnClickListener(new OnClickListener() {
								@Override
								public void onClick(View v) {
									onClickMotionIcon(v);
								}
							});
							DemoMakeActivity.this.runOnUiThread(new Runnable() {
								@Override
								public void run() {
									DemoMakeActivity.this.tagged_demo_button
											.addView(bt);
								}
							});
						}
					}
				}
			}).start();
		}

	}

	public void onClickMotionIcon(final View v) {
		this.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				DemoMakeActivity.this.selected_motion_view.setBackgroundColor(Color.GRAY);
				DemoMakeActivity.this.selected_motion_view = v;
				DemoMakeActivity.this.selected_motion_view.setBackgroundColor(Color.GREEN);
				//DemoMakeActivity.this.selected_motion_layout.removeAllViews();
				//DemoMakeActivity.this.selected_motion_layout.addView(v);
			}
		});
	}

	@Override
	public void onPause() {
		super.onPause();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	@Override
	public void finalize() throws Throwable {
		onDestroy();
		super.finalize();
	}

}
