package org.ros.android.chat;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.ImageView.ScaleType;

import org.ros.android.chat.R;
import org.ros.android.chat.RobotBarNode.TaggedIcon;

public class DemoMakeActivity extends Activity {

	final private String TAG = "DemoMakeActivity";

	private EditText demo_title_edit, voice_text_edit;
	private ImageButton back_button;
	private Button move_to_pose_button;
	private ImageButton register_button;

	private LinearLayout tagged_motion_button_layout;
	// private LinearLayout selected_motion_layout;
	private View selected_motion_view;
	private ArrayList<TaggedIcon> demo_icons;

	private ProgressDialog pDialog;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.demo_make_act);

		this.pDialog = new ProgressDialog(this);
		this.pDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		this.pDialog.setIndeterminate(false);
		this.pDialog.setCancelable(true);

		this.demo_icons = new ArrayList<TaggedIcon>();
		this.tagged_motion_button_layout = (LinearLayout) this
				.findViewById(R.id.taged_pose_image_buttons);
		// this.selected_motion_layout = (LinearLayout) this
		// .findViewById(R.id.selected_pose_image_view);
		this.selected_motion_view = this.tagged_motion_button_layout
				.getChildAt(0);
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

		this.demo_title_edit = (EditText) findViewById(R.id.demo_title_edit);
		this.voice_text_edit = (EditText) findViewById(R.id.voice_text_edit);
		this.register_button = (ImageButton) findViewById(R.id.demo_register_button);
		this.register_button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				DemoMakeActivity.this.pDialog.show();
				new Thread(new Runnable() {
					@Override
					public void run() {
						if (RobotBarActivity.rb_node != null
								&& DemoMakeActivity.this.demo_title_edit
										.getText().length() > 0) {
							byte[] icon = null;
							try {
								ImageButton ib = (ImageButton) DemoMakeActivity.this.selected_motion_view;
								BitmapDrawable bd = (BitmapDrawable) ib
										.getDrawable();
								Bitmap bm = bd.getBitmap();
								ByteArrayOutputStream baos = new ByteArrayOutputStream();
								bm.compress(CompressFormat.PNG, 50, baos);
								icon = baos.toByteArray();
							} catch (Exception e) {
							}
							String voice_text = DemoMakeActivity.this.voice_text_edit
									.getText().toString();
							boolean error = true;
							if (voice_text.length() > 0) {
								error = error & RobotBarActivity.rb_node.registerSound(
										DemoMakeActivity.this.demo_title_edit
												.getText().toString(),
										voice_text, null, null);
							}
							error = error & RobotBarActivity.rb_node.registerDemo(
									DemoMakeActivity.this.demo_title_edit
											.getText().toString(), icon,
									DemoMakeActivity.this.selected_motion_view
											.getTag().toString(),
									DemoMakeActivity.this.demo_title_edit
											.getText().toString());
							if ( ! error ){
								DemoMakeActivity.this.runOnUiThread(
										new Runnable(){
											@Override
											public void run(){
												Toast.makeText(DemoMakeActivity.this, "error", Toast.LENGTH_LONG).show();
											}});
							}
							DemoMakeActivity.this.pDialog.dismiss();
						}
					}
				}).start();
			}
		});
	}

	public void updateMotionIcons() {
		if (RobotBarActivity.rb_node != null) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					int cnt = RobotBarActivity.rb_node.getNewDemos(
							DemoMakeActivity.this.demo_icons,
							RobotBarNode.motion_head_string);
					if ( cnt < 0 ){
						DemoMakeActivity.this.runOnUiThread(
								new Runnable(){
									@Override
									public void run(){
										Toast.makeText(DemoMakeActivity.this, "error", Toast.LENGTH_LONG).show();
									}});
					}
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
									DemoMakeActivity.this.tagged_motion_button_layout
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
									DemoMakeActivity.this.tagged_motion_button_layout
											.addView(bt);
								}
							});
						}
					}
				}
			}).start();
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		updateMotionIcons();
	}

	public void onClickMotionIcon(final View v) {
		this.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				DemoMakeActivity.this.selected_motion_view
						.setBackgroundColor(Color.GRAY);
				DemoMakeActivity.this.selected_motion_view = v;
				DemoMakeActivity.this.selected_motion_view
						.setBackgroundColor(Color.GREEN);
				// DemoMakeActivity.this.selected_motion_layout.removeAllViews();
				// DemoMakeActivity.this.selected_motion_layout.addView(v);
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
