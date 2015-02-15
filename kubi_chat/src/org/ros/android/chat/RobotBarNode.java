package org.ros.android.chat;

import java.util.ArrayList;

import org.apache.ws.commons.util.Base64;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.parameter.ParameterTree;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class RobotBarNode extends AbstractNodeMain {

	final private String TAG = "RobotBarNode";
	private Context context;
	private ParameterTree rosparam;
	final private static String demo_head_string = "/robot_bar/demo";

	private String[] default_motion_tag = new String[] { "fuza1", "fuza2",
			"my1", "my2", "my3", "my4" };

	public RobotBarNode(Context con) {
		this.context = con;
	}

	@Override
	public GraphName getDefaultNodeName() {
		return GraphName.of("robot_bar_node");
	}

	@Override
	public void onStart(final ConnectedNode connectedNode) {
		this.rosparam = connectedNode.getParameterTree();

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
		int ret = 0;
		if (this.rosparam != null && this.rosparam.has(demo_head_string + "/tag")) {
			String tags = this.rosparam.getString(demo_head_string + "/tag");
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
					oldDemo.add(this.genTaggedIconWithTag(tag));
				}
			}
		}
		return ret;
	}

	public void registerDemo(String tag, byte[] icon, String mtag, String stag) {
		if ( this.rosparam == null || tag == null ) return ;
		if (icon != null) {
			this.rosparam.set(demo_head_string + "/" + tag + "/icon",
					Base64.encode(icon));
		}
		if (mtag != null) {
			this.rosparam.set(demo_head_string + "/" + tag + "/motion", mtag);
		}
		if (stag != null) {
			this.rosparam.set(demo_head_string + "/" + tag + "/sound", stag);
		}
		String tags;
		if (!this.rosparam.has(demo_head_string + "/tag")) {
			tags = tag;
		} else {
			tags = this.rosparam.getString(demo_head_string + "/tag") + " "
					+ tag;
		}
		this.rosparam.set(demo_head_string + "/tag", tags);
	}

	public TaggedIcon genTaggedIconWithTag(String tag) {
		String data;
		if (this.rosparam != null && this.rosparam.has(demo_head_string + "/" + tag + "/icon")) {
			data = this.rosparam.getString(demo_head_string + "/" + tag
					+ "/icon");
			try {
				byte[] icon = Base64.decode(data);
				return new TaggedIcon(tag, BitmapFactory.decodeByteArray(icon,
						0, icon.length));
			} catch (Exception e) {
			}
		}
		return new TaggedIcon(tag, null);
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

		public TaggedIcon(String tag, Bitmap icon) {
			this.tag = tag;
			this.icon = icon;
		}
	}
}
