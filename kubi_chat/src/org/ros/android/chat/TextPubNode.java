package org.ros.android.chat;

import java.nio.ByteOrder;

import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.NodeMain;
import org.ros.node.topic.Publisher;
import org.ros.node.topic.Subscriber;

import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.media.AudioFormat;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;

import org.ros.node.*;

public class TextPubNode implements NodeMain {

	private String package_name;
	private EditText edit_text;

	private Publisher<std_msgs.String> text_pub;

	@Override
	public GraphName getDefaultNodeName() {
		return GraphName.of(this.package_name+"/text");
	}

	public TextPubNode(String package_name, EditText edit_text) {
		this.package_name = package_name;
		this.edit_text = edit_text;
	}

	@Override
	public void onStart(final ConnectedNode connectedNode) {
		this.text_pub = connectedNode.newPublisher(this.package_name
				+ "/text/string", "std_msgs/String");
		this.edit_text
				.setOnEditorActionListener(new EditText.OnEditorActionListener() {
					@Override
					public boolean onEditorAction(TextView v, int actionId,
							KeyEvent event) {
						if (actionId == EditorInfo.IME_ACTION_SEARCH
								|| actionId == EditorInfo.IME_ACTION_DONE
								|| event.getAction() == KeyEvent.ACTION_DOWN
								&& event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
							if (event == null || !event.isShiftPressed()) {
								std_msgs.String msg = TextPubNode.this.text_pub.newMessage();
								msg.setData(TextPubNode.this.edit_text.getText().toString());
								TextPubNode.this.text_pub.publish(msg);
								TextPubNode.this.edit_text.setText("");
								return true; 
							}
						}
						return false;
					}
				});
	}

	@Override
	public void onError(Node arg0, Throwable arg1) {
	}

	@Override
	public void onShutdown(Node arg0) {
	}

	@Override
	public void onShutdownComplete(Node arg0) {
	}

}