package org.ros.android.chat;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import org.ros.android.chat.R;

public class DemoMakeActivity extends Activity{

	final private String TAG = "DemoMakeActivity";
	
	private EditText edit_text;
	private ImageButton back_button;
	private Button move_to_pose_button;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.demo_make_act);
		
		this.back_button = (ImageButton)findViewById(R.id.demo_craete_back_to_home_button);
		this.back_button.setOnClickListener( new OnClickListener(){
			@Override
			public void onClick(View v) {
				DemoMakeActivity.this.finish();
			}
		} );
		
		this.move_to_pose_button = (Button)findViewById(R.id.pose_craete_move_button);
		this.move_to_pose_button.setOnClickListener( new OnClickListener(){
			@Override
			public void onClick(View v) {
				Intent i = new Intent(DemoMakeActivity.this.getApplicationContext(), PoseMakeActivity.class);
				DemoMakeActivity.this.startActivity(i);
			}
		} );
	}

	@Override
	public void onResume() {
		super.onResume();
	}
	
	@Override
	public void onPause() {
		super.onPause();
	}
	
	@Override
	public void onDestroy(){
		super.onDestroy() ;
	}
	
	@Override
	public void finalize() throws Throwable{
		onDestroy();
		super.finalize();
	}
	
}
