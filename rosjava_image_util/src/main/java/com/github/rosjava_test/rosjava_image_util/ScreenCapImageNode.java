package com.github.rosjava_test.rosjava_image_util;

import java.awt.AWTException;
import java.awt.HeadlessException;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.ros.node.ConnectedNode;
import org.ros.node.Node;

public class ScreenCapImageNode extends SensorImageNode implements Runnable {

	private Thread thread;
	
	public ScreenCapImageNode (){
		super();
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
		long sleep_time = 100;
		long now;
		while ( this.thread != null ){
			try {
				now = System.currentTimeMillis();
				BufferedImage image = new Robot().createScreenCapture(new Rectangle(Toolkit.getDefaultToolkit().getScreenSize()));
				if ( image != null ){
					ImageIO.write(image, "png", new File("/tmp/screen_cap" + ".png"));
					publishCompressedImage("/tmp/screen_cap" + ".png");
				} else {
					System.out.println("[ScreenCapImageNode] null image");
				}
				if ( (now = System.currentTimeMillis() - now) > 0 ){
					Thread.sleep(now);
				} else {
					System.out.println("[ScreenCapImageNode] overslept");
				}
			} catch (IOException e) {
				e.printStackTrace();
			} catch (HeadlessException e) {
				e.printStackTrace();
			} catch (AWTException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	};

}
