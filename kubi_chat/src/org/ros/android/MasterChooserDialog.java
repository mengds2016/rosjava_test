package org.ros.android;

import org.ros.android.chat.R;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class MasterChooserDialog extends Dialog{

	private EditText master_uri, hostname, nodename ;
	private Button ok, ng ;
	private boolean isOk ;
		
	public MasterChooserDialog(Context context, String user, String pass){
		this(context,user,pass,"");
	}
	
	public MasterChooserDialog(Context context, String user, String pass, String nodeName) {
		super(context);
		this.setContentView(R.layout.master_chooser_dialog) ;
		
		this.setTitle("Login") ;
		this.master_uri = (EditText)this.findViewById(R.id.master_chooser_dialog_uri) ;
		this.hostname = (EditText)this.findViewById(R.id.master_chooser_dialog_hostname) ;
		this.nodename = (EditText)this.findViewById(R.id.master_chooser_dialog_nodename) ;
		this.ok = (Button)this.findViewById(R.id.certification) ;
		this.ng = (Button)this.findViewById(R.id.cancel) ;
		this.isOk = false ;
		
		this.master_uri.setText(user) ;
		this.hostname.setText(pass) ;
		this.nodename.setText(nodeName) ;
		
		this.ok.setOnClickListener( new android.view.View.OnClickListener(){
			@Override
			public void onClick(View v) {
				MasterChooserDialog.this.isOk = true ;
				MasterChooserDialog.this.dismiss() ;
			}
		} ) ;
		
		this.ng.setOnClickListener( new android.view.View.OnClickListener(){
			@Override
			public void onClick(View v) {
				MasterChooserDialog.this.isOk = false ;
				MasterChooserDialog.this.dismiss() ;
			}
		} ) ;
	}
	
	public boolean isOK() {
		return this.isOk ;
	}
	
	public String getMasterUri() {
		if ( this.isOk ){
			return this.master_uri.getText().toString() ;
		} else {
			return "" ;
		}
	}
	
	public String getHostname() {
		if ( this.isOk ){
			return this.hostname.getText().toString() ;
		} else {
			return "" ;
		}
	}
	
	public String getNodename() {
		if ( this.isOk ){
			return this.nodename.getText().toString() ;
		} else {
			return "" ;
		}
	}
}
