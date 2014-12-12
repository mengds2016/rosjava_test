package com.github.rosjava_test.rosjava_image_util;

import java.awt.AWTException;
import java.awt.HeadlessException;
import java.awt.MouseInfo;
import java.awt.PointerInfo;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.ros.node.ConnectedNode;
import org.ros.node.Node;

public class HalfScreenCapImageNode extends SensorImageNode implements Runnable {

	private Thread thread;
	private BufferedImage pointer;
	private Robot robot;
	
	public HalfScreenCapImageNode (){
		super();
		String home_dir = System.getenv("IMG_HOME"); 
		if ( home_dir == null ){
			home_dir = System.getenv("HOME") + "/prog/euslib/demo/s-noda/tmp-ros-package/rosjava_test/rosjava_image_util/img";
		}
		try {
			this.robot = new Robot();
			this.pointer = ImageIO.read(new File(home_dir+"/pointer.png"));
		} catch (IOException e) {
			e.printStackTrace();
		} catch (AWTException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void onShutdown(Node node){
		super.onShutdown(node);
		try {
			finalize();
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void onStart(final ConnectedNode connectedNode) {
		super.onStart(connectedNode);
		this.thread = new Thread(this);
		this.thread.start();
	}
	
	@Override
	public void finalize() throws Throwable{
		super.finalize();
		this.thread = null ;
	}

	@Override
	public void run() {
		long sleep_time = 50;
		long now;
		this.robot.setAutoDelay((int)sleep_time);
		while ( this.thread != null && this.robot != null){
			try {
				now = System.currentTimeMillis();
				Rectangle rect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
				rect.x = rect.width/2;
				rect.width = rect.width/2;
				BufferedImage image = this.robot.createScreenCapture(rect);
				if ( image != null ){
//					if (this.buf == null
//							|| this.buf.getWidth() != image.getWidth() / 2
//							|| this.buf.getHeight() != image.getHeight()) {
//						this.buf = new BufferedImage(image.getWidth()/2, image.getHeight(), image.getType());
//					}
//					this.buf.setData( image.getData(new Rectangle(this.buf.getWidth(),0,this.buf.getWidth()*2,this.buf.getHeight())));
					if (this.pointer != null) {
						PointerInfo pointer = MouseInfo.getPointerInfo();
						int x = (int) pointer.getLocation().getX();
						int y = (int) pointer.getLocation().getY();
						image.getGraphics().drawImage(this.pointer,
								x - this.pointer.getWidth() / 2 - rect.x,
								y - this.pointer.getHeight()/2 - rect.y, null);
					}
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					// ImageIO.write(image.getSubimage(image.getWidth()/2, 0, image.getWidth()/2, image.getHeight()), "png", baos);
					ImageIO.write(image, "png", baos);
					byte[] bytes = baos.toByteArray();
					// ImageIO.write(image.getSubimage(image.getWidth()/2, 0, image.getWidth()/2, image.getHeight()), "png", new File("/tmp/screen_cap" + ".png"));
					//ImageIO.write(this.buf, "png", new File("/tmp/screen_cap" + ".png"));
					// publishCompressedImage("/tmp/screen_cap" + ".png");
					publishCompressedImage(bytes);
				} else {
					System.out.println("[ScreenCapImageNode] null image");
				}
				if ( (now = sleep_time - (System.currentTimeMillis() - now)) > 0 ){
					Thread.sleep(now);
				} else {
					System.out.println("[ScreenCapImageNode] overslept");
				}
			} catch (IOException e) {
				e.printStackTrace();
			} catch (HeadlessException e) {
				e.printStackTrace();
			//} catch (AWTException e) {
			//	e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	};

}
