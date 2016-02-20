package org.ros.android.multi_touch_view;

import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.NodeMain;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class MultiTouchView extends SurfaceView implements NodeMain, Runnable,
		SurfaceHolder.Callback {

	private String topicName;
	private MultiTouchTalker talker;

	private float[][] presure_map;
	private myTouchEvent[] prev_touch_event ;
	private myTouchEvent[] touch_events;
	private int pixel_step;

	private SurfaceHolder surface ;
	private Thread draw_thread ;
	
	private boolean drawing ;
	private float max_frame_rate = 100f ;
	private float animation_time_step = 100f; // milli sec
	private long last_anime_time = 0;
	
	public MultiTouchView(Context context) {
		super(context);
	}

	public void setTalker(MultiTouchTalker talker) {
		this.talker = talker;
	}

	public MultiTouchView(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.surface = this.getHolder() ;
		this.surface.addCallback(this);
		this.pixel_step = 50;
	}

	public MultiTouchView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		this.surface = this.getHolder() ;
		this.surface.addCallback(this);
		this.pixel_step = 50;
	}

	public void setTopicName(String topicName) {
		this.topicName = topicName;
	}
	
	public void pressure_draw( float[][] pre_map ){
		Paint paint = new Paint();

		Canvas canvas = this.surface.lockCanvas();
		if ( canvas != null ){
			for ( int w=0 ; w<pre_map[0].length ; w++ ){
				for ( int h=0 ; h<pre_map.length ; h++ ){
					float x = w * this.pixel_step ;
					float y = h * this.pixel_step ;
					float hue = pre_map[h][w] * 300 ;
					paint.setColor(Color.HSVToColor( new float[]{hue,0.5f,0.5f} )) ;
					canvas.drawRect(x, y, x + this.pixel_step, y + this.pixel_step, paint) ;
				}
			}
			this.surface.unlockCanvasAndPost(canvas);
		}
	}

	@Override
	public GraphName getDefaultNodeName() {
		return GraphName.of("turtlebot_controller/compressed_image_view");
	}

	@Override
	public void onStart(ConnectedNode connectedNode) {
		// Subscriber<sensor_msgs.CompressedImage> subscriber = connectedNode
		// .newSubscriber(topicName, sensor_msgs.CompressedImage._TYPE);
		// subscriber
		// .addMessageListener(new
		// MessageListener<sensor_msgs.CompressedImage>() {
		// @Override
		// public void onNewMessage(
		// final sensor_msgs.CompressedImage message) {
		// System.out.println( "receive message" ) ;
		// post(new Runnable() {
		// @Override
		// public void run() {
		// ChannelBuffer buffer = message.getData();
		// byte[] data = buffer.array();
		// if (MultiTouchView.this.showBitmap != null) {
		// MultiTouchView.this.showBitmap
		// .recycle();
		// }
		// MultiTouchView.this.showBitmap = BitmapFactory
		// .decodeByteArray(data,
		// buffer.arrayOffset(),
		// buffer.readableBytes());
		// }
		// });
		// postInvalidate();
		// }
		// });
	}

	@Override
	public boolean onTouchEvent(MotionEvent e) {
		myTouchEvent[] touch_events = new myTouchEvent[e.getPointerCount()];
		System.out.println("[TouchEvent]") ;
		for (int i = 0; i < touch_events.length; i++) {
			touch_events[i] = new myTouchEvent(e.getX(i), e.getY(i),
					e.getPressure(i));
			System.out.printf("  [%d]: %f %f %f\n", i, e.getX(i), e.getY(i), e.getPressure(i) ) ;
		}
		this.touch_events = touch_events ;
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

	// SurfaceHolder.Callback
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		this.presure_map = new float[height / this.pixel_step][width / this.pixel_step];
		this.drawing = true ;
		this.draw_thread = new Thread(this);
		this.draw_thread.start();
	}
	
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
//		Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
//		paint.setStyle(Style.FILL);
//
//		Canvas canvas = holder.lockCanvas();
//		canvas.drawColor(Color.BLACK);
//		paint.setColor(Color.BLUE);
//		canvas.drawCircle(100, 200, 50, paint);
//		paint.setColor(Color.WHITE);
//		canvas.drawText("READY...", 100, 200, paint) ;
//		holder.unlockCanvasAndPost(canvas);
		//
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		this.drawing = false ;
		this.draw_thread = null;
	}

	class myTouchEvent {
		public float x, y;
		public float pressure;

		public myTouchEvent(float x, float y, float pressure) {
			this.x = x;
			this.y = y;
			this.pressure = pressure;
		}
	}
	
	public static float[] floatArray( myTouchEvent[] elist ){
		float[] ret = new float[ elist.length * 3 ];
		for ( int i=0 ; i<elist.length ; i++ ){
			ret[3*i] = elist[i].x ;
			ret[3*i+1] = elist[i].y ;
			ret[3*i+2] = elist[i].pressure ;
		}
		return ret ;
	}
	
	@Override
	public void run() {
		long start_time ;
		long end_time ;
		while ( this.drawing ){
			
			start_time = System.currentTimeMillis() ;
			
			if ( this.prev_touch_event != this.touch_events ){
				this.talker.publish( MultiTouchView.floatArray(this.touch_events) ) ;
				this.prev_touch_event = this.touch_events ;
				for (int i = 0; i < this.prev_touch_event.length; i++) {
					int x = (int) (this.prev_touch_event[i].x / this.pixel_step);
					int y = (int) (this.prev_touch_event[i].y / this.pixel_step);
					float p = this.prev_touch_event[i].pressure;
					for (int w = 0; w < this.presure_map[0].length; w++) {
						for (int h = 0; h < this.presure_map.length; h++) {
							if (i == 0)
								this.presure_map[h][w] *= 0.6f;
							this.presure_map[h][w] += p
									* Math.exp(-1e-4
											* this.pixel_step
											* this.pixel_step
											* ((w - x) * (w - x) + (h - y)
													* (h - y)));
						}
					}
				}
			} else {
				for (int w = 0; w < this.presure_map[0].length; w++) {
					for (int h = 0; h < this.presure_map.length; h++) {
						this.presure_map[h][w] *= 0.8f ;
					}
				}
			}
			
			if ( start_time - this.last_anime_time > this.animation_time_step ) {
				this.last_anime_time = start_time;
				this.pressure_draw(this.presure_map);
			}
			
			end_time = System.currentTimeMillis() ;
			if ( end_time - start_time < (1e+3 / this.max_frame_rate) ){
				try {
					Thread.sleep((long) ((1e+3 / this.max_frame_rate) - (end_time - start_time)));
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			System.out.println("[FRAME RATE] "
					+ (1e+3 / (System.currentTimeMillis() - start_time)) + "[fps]"
					+ " >> " + (end_time - start_time) + "[ms] vs "
					+ (1e+3 / this.max_frame_rate) + "[ms]");
		}
	}
}
