package org.ros.android;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnDismissListener;
import android.os.AsyncTask;
import org.ros.address.InetAddressFactory;
import org.ros.exception.RosRuntimeException;
import org.ros.node.NodeMainExecutor;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;

public abstract class RosDialogActivity extends RosActivity {

	private SharedPreferences pref;
	private SharedPreferences.Editor editor;

	private final static String preftag = "RosDialogActivity";
	private final static String preftag_master = "ROS_MASTER_URI";
	private final static String preftag_hostname = "ROS_HOSTNAME";
	private final static String preftag_nodename = "ROS_NODENAME";
	
	private String hostname ;
	protected String nodename_org="";

	protected RosDialogActivity(String notificationTicker,
			String notificationTitle, String nodename_org) {
		this(notificationTicker, notificationTitle);
		this.nodename_org = nodename_org;
	}
	
	protected RosDialogActivity(String notificationTicker,
			String notificationTitle) {
		super(notificationTicker, notificationTitle);
	}

	public String getHostname() {
		if (this.hostname != null) {
			return this.hostname;
		} else {
			InetAddress ros_ip;
			try {
				ros_ip = InetAddressFactory.newNonLoopback();
			} catch (RosRuntimeException e) {
				ros_ip = InetAddressFactory.newLoopback();
				e.printStackTrace();
			}
			return (this.hostname = ros_ip.getHostAddress());
		}
	}
	
	public String getNodename(){
		return this.nodename_org;
		//this.nodename_org;
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	protected abstract void init(NodeMainExecutor nodeMainExecutor);

	@Override
	public void startMasterChooser() {

		this.pref = getSharedPreferences(RosDialogActivity.preftag,
				Context.MODE_PRIVATE);
		
		String master_uri = this.pref
				.getString(RosDialogActivity.preftag_master, "");
		if (master_uri.length() == 0)
			master_uri = "http://localhost:11311";
		
		String node_name = this.pref
				.getString(RosDialogActivity.preftag_nodename, this.nodename_org);
		final MasterChooserDialog ld = new MasterChooserDialog(this,
				master_uri, getHostname(), node_name);
		ld.setOnDismissListener(new OnDismissListener() {
			@Override
			public void onDismiss(DialogInterface dialog) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						if (ld.isOK()) {
							String master = ld.getMasterUri();
							RosDialogActivity.this.hostname = ld.getHostname() ;
							editor = pref.edit();
							editor.putString(RosDialogActivity.preftag_master, master) ;
							editor.putString(
									RosDialogActivity.preftag_hostname,
									ld.getHostname());
							editor.putString(
									RosDialogActivity.preftag_nodename,
									ld.getNodename());
							RosDialogActivity.this.nodename_org = ld.getNodename();
							editor.commit();
							//
							if (master == null || master.length() == 0
									|| master.contains("localhost")
									|| master.contains("127.0.0.1")) {
								nodeMainExecutorService.startMaster();
							} else {
								URI uri;
								try {
									uri = new URI(master);
								} catch (URISyntaxException e) {
									throw new RosRuntimeException(e);
								}
								nodeMainExecutorService.setMasterUri(uri);
							}
							new AsyncTask<Void, Void, Void>() {
								@Override
								protected Void doInBackground(Void... params) {
									RosDialogActivity.this
											.init(nodeMainExecutorService);
									return null;
								}
							}.execute();
						} else{
							RosDialogActivity.this.finish();
						}
					}
				}).start();
			}
		});
		ld.show();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
	}
}
