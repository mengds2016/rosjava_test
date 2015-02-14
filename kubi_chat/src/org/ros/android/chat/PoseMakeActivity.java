package org.ros.android.chat;


import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageButton;

import org.ros.android.chat.R;

public class PoseMakeActivity extends Activity{

	final private String TAG = "PoseMakeActivity";
	
	private EditText edit_text;
	private ImageButton back_button;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.pose_make_act);
		
		this.back_button = (ImageButton)findViewById(R.id.pose_craete_back_to_home_button);
		this.back_button.setOnClickListener( new OnClickListener(){
			@Override
			public void onClick(View v) {
				PoseMakeActivity.this.finish();
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
