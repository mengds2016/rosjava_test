package org.ros.android.chat;


import org.jboss.netty.buffer.ChannelBuffer;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.NodeMain;
import org.ros.node.topic.Subscriber;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.ImageView;

public class CompressedImageView extends ImageView implements NodeMain {

	private String topicName;
	private String messageType;
	private Bitmap showBitmap;
	private float aspect;
	private RosChatNode talker ;
	private RosChatNode chat ;
	private String nodename = RosChatActivity.node_name + "/compressed_image_view";


	public CompressedImageView(Context context) {
		super(context);
	}
	
	public CompressedImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public CompressedImageView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public void setTopicName(String topicName) {
		this.topicName = topicName;
	}

	public void setMessageType(String messageType) {
		this.messageType = messageType;
	}
	
	public void setTalker( RosChatNode talker ){
		this.talker = talker ;
	}

	public void setChat(RosChatNode chat){
		this.chat = chat ;
	}
	
	public void setNodeName(String name){
		
	}
	
	@Override
	public GraphName getDefaultNodeName() {
		return GraphName.of( this.nodename );
	}

	@Override
	public void onStart(ConnectedNode connectedNode) {
		Subscriber<sensor_msgs.CompressedImage> subscriber = connectedNode
				.newSubscriber(this.topicName, this.messageType);
		subscriber
				.addMessageListener(new MessageListener<sensor_msgs.CompressedImage>() {
					@Override
					public void onNewMessage(
							final sensor_msgs.CompressedImage message) {
						post(new Runnable() {
							@Override
							public void run() {
								ChannelBuffer buffer = message.getData();
								byte[] data = buffer.array();
								if (CompressedImageView.this.showBitmap != null) {
									CompressedImageView.this.showBitmap
											.recycle();
								}
								CompressedImageView.this.showBitmap = BitmapFactory
										.decodeByteArray(data,
												buffer.arrayOffset(),
												buffer.readableBytes());
								CompressedImageView.this.setBitmap();
								setImageBitmap(CompressedImageView.this.showBitmap);
							}
						});
						postInvalidate();
					}
				}, 1);
	}

	public void setBitmap() {
		setBitmap(this.showBitmap);
	}

	public void setBitmap(Bitmap bmp) {
		this.showBitmap = bmp ;
		float aspect = 1.0f * bmp.getWidth() / bmp.getHeight();
		if (Math.abs(this.aspect - aspect) > 0.01) {
			this.aspect = aspect;
			float rate = //1.0f ;
					Math.min(1.0f * this.getWidth() / bmp.getWidth(),
					1.0f * this.getHeight() / bmp.getHeight());
			ViewGroup.LayoutParams param = CompressedImageView.this
					.getLayoutParams();
			param.width = (int) (rate * bmp.getWidth());
			param.height = (int) (rate * bmp.getHeight());
			this.setLayoutParams(param);
		}
		setImageBitmap(bmp);
		postInvalidate();
	}

	@Override
	public boolean onTouchEvent(MotionEvent e) {
		float[] touch_events = new float[e.getPointerCount()*3];
		//System.out.println("[TouchEvent]") ;
		for (int i = 0; i < touch_events.length/3; i++) {
			touch_events[i*3+0] = e.getX(i) ;
			touch_events[i*3+1] = e.getY(i) ;
			touch_events[i*3+2] = e.getPressure(i) ;
			//System.out.printf("  [%d]: %f %f %f\n", i, e.getX(i), e.getY(i), e.getPressure(i) ) ;
			if ( this.chat != null ){
				float pan = 10f * (e.getX(i) - this.getWidth()/2) / this.getWidth()   ;
				float tlt = -10f * (e.getY(i) - this.getHeight()/2) / this.getHeight();
				System.out.println( "[compressed image view] move command " + pan + "x" + tlt ) ;
			} else {
				System.out.println( "[compressed image view] chat = null!!" ) ;
			}
		}
		if ( this.talker != null ) {
			this.talker.touchEventPublish(touch_events) ;
		}
		return true;
	}

	@Override
	public void onShutdown(Node node) {
	}

	@Override
	public void onShutdownComplete(Node node) {
	}

	@Override
	public void onError(Node node, Throwable throwable) {
	}
}
