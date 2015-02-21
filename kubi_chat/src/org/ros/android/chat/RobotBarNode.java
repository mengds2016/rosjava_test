package org.ros.android.chat;

import java.util.ArrayList;

import org.apache.ws.commons.util.Base64;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.parameter.ParameterTree;
import org.ros.node.topic.Publisher;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class RobotBarNode extends AbstractNodeMain {

	final private String TAG = "RobotBarNode";
	private RobotBarActivity context;
	private ParameterTree rosparam;
	final public static String demo_head_string = "/robot_bar/demo";
	final public static String motion_head_string = "/robot_bar/motion";
	final public static String sound_head_string = "/robot_bar/sound";

	private Publisher<std_msgs.String> string_publisher;
	private String nodename;

	// private String[] default_motion_tag = new String[] { "fuza1", "fuza2",
	// "my1", "my2", "my3", "my4" };

	public RobotBarNode(RobotBarActivity con) {
		this.context = con;
		this.nodename = RobotBarActivity.node_name + "/robot_bar_node";
	}

	@Override
	public GraphName getDefaultNodeName() {
		return GraphName.of(this.nodename);
	}

	public void publishStringStatus(String str) {
		if (this.string_publisher != null) {
			std_msgs.String msg = this.string_publisher.newMessage();
			msg.setData(str);
			this.string_publisher.publish(msg);
		}
	}

	@Override
	public void onStart(final ConnectedNode connectedNode) {
		this.rosparam = connectedNode.getParameterTree();
		this.context.updateDemoIcons();

		this.string_publisher = connectedNode.newPublisher(this.nodename
				+ "/status/string", std_msgs.String._TYPE);
		// for (String imageName : this.default_motion_tag) {
		// R.drawable rDrawable = new R.drawable();
		// Field field;
		// int resId;
		// try {
		// field = rDrawable.getClass().getField(imageName);
		// resId = field.getInt(rDrawable);
		// Bitmap image = BitmapFactory.decodeResource(
		// this.context.getResources(), resId);
		// image.recycle();
		// image = null;
		// } catch (Exception e) {
		// }
		// }
	}

	public int getNewDemos(ArrayList<TaggedIcon> oldDemo) {
		return getNewDemos(oldDemo, demo_head_string);
	}

	public int getNewDemos(ArrayList<TaggedIcon> oldDemo, String head) {
		int ret = 0;
		try {
			if (this.rosparam != null && this.rosparam.has(head + "/tag")) {
				String tags = this.rosparam.getString(head + "/tag");
				String[] tags_array = tags.split(" ");
				for (String tag : tags_array) {
					boolean isOld = false;
					for (TaggedIcon old : oldDemo) {
						if (old.tag.contentEquals(tag)) {
							isOld = true;
							break;
						}
					}
					if (!isOld) {
						ret++;
						oldDemo.add(0, this.genTaggedIconWithTag(tag, head));
					}
				}
			}
		} catch (Exception e) {
			ret = -1;
		}
		return ret;
	}

	public String serializedString(String in) {
		StringBuilder buf = new StringBuilder();
		for (int i = 0; i < in.length(); i++) {
			int num = (int) in.charAt(i);
			buf.append(String.format("%04x", num));
		}
		return buf.toString();
	}

	public boolean registerDemo(String tag, byte[] icon, String mtag,
			String stag) {
		if (this.rosparam == null || tag == null)
			return false;
		String name = tag;
		tag = serializedString(tag);
		try {
			if (name != null) {
				this.rosparam.set(demo_head_string + "/" + tag + "/name", name);
			}
			if (icon != null) {
				this.rosparam.set(demo_head_string + "/" + tag + "/icon",
						Base64.encode(icon));
			}
			if (mtag != null) {
				// mtag = serializedString(mtag);
				this.rosparam.set(demo_head_string + "/" + tag + "/motion",
						mtag);
			}
			if (stag != null) {
				stag = serializedString(stag);
				this.rosparam
						.set(demo_head_string + "/" + tag + "/sound", stag);
			}
			String tags;
			if (!this.rosparam.has(demo_head_string + "/tag")) {
				tags = tag;
			} else {
				tags = this.rosparam.getString(demo_head_string + "/tag") + " "
						+ tag;
			}
			this.rosparam.set(demo_head_string + "/tag", tags);
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	public boolean registerSound(String tag, String voice_text, byte[] data,
			byte[] icon) {
		if (this.rosparam == null || tag == null)
			return false;
		String name = tag;
		tag = serializedString(tag);
		try {
			if (name != null) {
				this.rosparam
						.set(sound_head_string + "/" + tag + "/name", name);
			}
			if (icon != null) {
				this.rosparam.set(sound_head_string + "/" + tag + "/icon",
						Base64.encode(icon));
			}
			if (voice_text != null) {
				this.rosparam.set(sound_head_string + "/" + tag + "/text",
						voice_text);
			}
			if (data != null) {
				this.rosparam.set(sound_head_string + "/" + tag + "/data",
						Base64.encode(data));
			}
			String tags;
			if (!this.rosparam.has(sound_head_string + "/tag")) {
				tags = tag;
			} else {
				tags = this.rosparam.getString(sound_head_string + "/tag")
						+ " " + tag;
			}
			this.rosparam.set(sound_head_string + "/tag", tags);
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	public TaggedIcon genTaggedIconWithTag(String tag, String head)
			throws Exception {
		String data;
		Bitmap img = null;
		String name = tag;
		if (this.rosparam != null) {
			if (this.rosparam.has(head + "/" + tag + "/icon")) {
				data = this.rosparam.getString(head + "/" + tag
						+ "/icon");
				byte[] icon = Base64.decode(data);
				BitmapFactory.Options options = new  BitmapFactory.Options();
				// options.inMutable = true;
				img = BitmapFactory.decodeByteArray(icon, 0, icon.length,options);
			}
			if (this.rosparam.has(head + "/" + tag + "/name")) {
				name = this.rosparam.getString(head + "/" + tag
						+ "/name");
			}
		}
		return new TaggedIcon(tag, img, name);
	}

	public void onDestroy() {
	}

	@Override
	public void onShutdown(Node node) {
		super.onShutdownComplete(node);
		this.onDestroy();
	}

	public class TaggedIcon {
		public String tag;
		public Bitmap icon;
		public String name;

		public TaggedIcon(String tag, Bitmap icon, String name) {
			this.tag = tag;
			this.icon = icon;
			this.name = name;
		}
	}
}
