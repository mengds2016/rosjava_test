package com.github.rosjava_test.rosjava_image_util;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.ros.node.topic.Publisher;

public class ImageWindowSampleFrame extends JFrame {
	private static final long serialVersionUID = 1L;

	private static int W = 480*2, H = 640;

	private BorderLayout camera_layout;
	private BorderLayout outer_layout;
	private JPanel camera_pane;

	public ImageView leftCameraView, rightCameraView;
	public CommandView commandView;

	public ImageWindowSampleFrame() {
		this.camera_layout = new BorderLayout();
		this.outer_layout = new BorderLayout();
		this.camera_pane = new JPanel();
		this.camera_pane.setLayout(this.camera_layout);
		this.getContentPane().setLayout(this.outer_layout);

		this.commandView = new CommandView();
		this.leftCameraView = new ImageView(this.commandView, (W-4)/2, H-20);
		this.rightCameraView = new ImageView(this.commandView, (W-4)/2, H-20);

		this.camera_pane.add(this.leftCameraView, BorderLayout.CENTER);
		this.camera_pane.add(this.rightCameraView, BorderLayout.EAST);

		try {
			BufferedImage elbow1 = ImageIO.read(new File("/home/s-noda/prog/euslib/demo/s-noda/tmp-ros-package/rosjava_test/rosjava_image_util/img/elbow.png"));
			BufferedImage elbow2 = ImageIO.read(new File("/home/s-noda/prog/euslib/demo/s-noda/tmp-ros-package/rosjava_test/rosjava_image_util/img/elbow.png"));
			this.leftCameraView.pane.addImage("left_elbow", elbow1, W/(2*4), H/2, -1, -1);
			this.leftCameraView.pane.addImage("right_elbow", elbow2, 3*W/(2*4), H/2, -1, -1);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		this.add(this.camera_pane);
		this.add(this.commandView, BorderLayout.SOUTH);

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setTitle("ImageWindowSample");
		// this.setPreferredSize(new Dimension(W,H)) ;
		setSize(W, H);

		setBackground(Color.black);

		setVisible(true);
	}

	public void setLeftImage(BufferedImage i){
		this.leftCameraView.pane.setBgImage(i);
		this.repaint();
	}
	
	public void setRightImage(BufferedImage i){
		this.rightCameraView.pane.setBgImage(i);
		this.repaint();
	}

	public static void main(String[] args) {
		new ImageWindowSampleFrame().repaint();
	}

	// Comand View class
	//
	public class CommandView extends JLabel{
		private static final long serialVersionUID = 2L;
		public CommandView() {
			this.setBackground(Color.black) ;
			this.setForeground(Color.green) ;
			this.setOpaque(true) ;
			this.setText("STANDBY ... ") ;
			setVisible(true);
		}
	}
	
	
	public class ImageData {
		private BufferedImage image;
		public String name;
		public int x=0, y=0, w=100, h=100;	
		public boolean flush=false;
		
		public void setImage(BufferedImage i) {
			this.image = i;
			this.w = this.image.getWidth();
			this.h = this.image.getHeight();
		}

		public BufferedImage getImage() {
			return this.image;
		}
		
		public void drawBackground(Graphics g, int panel_w, int panel_h) {
			BufferedImage i = this.image;
			if (i != null) {
				double rate = Math.min(1.0 * panel_w / this.w, 1.0 * panel_h
						/ this.h);
				double woffset = (panel_w - this.w * rate) / 2;
				double hoffset = (panel_h - this.h * rate) / 2;
				g.drawImage(i, (int) (woffset), (int) (hoffset),
						(int) (panel_w - woffset * 2),
						(int) (panel_h - hoffset * 2), null);
			} else {
				g.clearRect(0, 0, panel_w, panel_h);
				g.drawString("NO IMAGE", panel_w / 2, panel_h / 2);
			}
		}
		
		public void draw(Graphics g) {
			BufferedImage i = this.image;
			if (i != null) {
				g.drawImage(i, this.x, this.y, this.w, this.h, null);
			}
			if ( this.flush ){
				g.setColor(Color.GREEN);
				g.drawRect(this.x, this.y, this.w, this.h);
			}
		}
		
//		public void red_filter(int col) {
//			if (this.image == null) {
//				return;
//			}
//			if (this.red_image != null && col == this.filter) {
//				return;
//			}
//			this.filter = col ;
//			this.red_image = new BufferedImage(this.image.getWidth(),
//					this.image.getHeight(), this.image.getType());
//			for (int x = 0; x < this.image.getWidth(); x++) {
//				for (int y = 0; y < this.image.getHeight(); y++) {
//					this.red_image.setRGB(x, y,
//							col & this.image.getRGB(x, y));
//				}
//			}
//		}
	}
	
	// Image Panel class
	//
	public class ImagePanel extends JPanel {
		private static final long serialVersionUID = 8L;

		//private BufferedImage image;
		private ImageData bgImage;
		private ArrayList<ImageData> images;
		private int w, h;

		public ImagePanel(){
			this(600,600);
		}
		
		public ImagePanel(int w, int h) {
			this.w = w ;
			this.h = h ;
			this.images = new ArrayList<ImageData>();
			this.bgImage = new ImageData();
			this.setPreferredSize(new Dimension(w, h));
		}
		
		public void addImage(String name, BufferedImage i, int x, int y, int w, int h){
			ImageData img = new ImageData();
			img.setImage(i);
			if ( x > 0 ) img.x = x;
			if ( y > 0 ) img.y = y;
			if ( w > 0 ) img.w = w;
			if ( h > 0 ) img.h = h;
			img.name = name;
			this.images.add(img);
		}

		public void setBgImage(BufferedImage i) {
			this.bgImage.setImage(i);
		}

		public BufferedImage getBgImage() {
			return this.bgImage.getImage();
		}
		
		@Override
		public void paintComponent(Graphics g) {
			this.bgImage.drawBackground(g, (this.w = this.getWidth()), (this.h = this.getHeight()));
			for ( ImageData d : this.images ){
				d.draw(g);
			}
		}

		@Override
		public void paint(Graphics g) {
			paintComponent(g);
		}
	}
	
	// ImageView class
	//
	public class ImageView extends JPanel implements MouseListener, MouseMotionListener {
		private static final long serialVersionUID = 3L;
		
		private GridLayout out;
		//final public String topic_raw, topic_compressed;
		final public ImagePanel pane;
		public int w, h;

		private JLabel prompt;
		private Publisher<std_msgs.String> event_publisher;
		
		ImageData selected;

		public ImageView(JLabel prompt, int w, int h) {
			this.prompt = prompt;
			this.out = new GridLayout(1, 1);
			this.pane = new ImagePanel(w, h);
			this.setLayout(this.out);
			this.add(this.pane);
			this.addMouseListener(this);
			this.addMouseMotionListener(this) ;
			setVisible(true);
		}
		
		public void setEventPublisher(Publisher<std_msgs.String> event) {
			this.event_publisher = event;
		}
		
		
		public boolean updateSelectedImage(int x, int y) {
			ImageData selected = null;
			for (int i = this.pane.images.size()-1 ; i>=0 ; i--  ) {
				ImageData img = this.pane.images.get(i) ;
				if ( selected != null ){
					img.flush = false;
				} else if (x > img.x && x < img.x + img.w && y > img.y
						&& y < img.y + img.h) {
					selected = img;
					selected.flush = true;
				} else {
					img.flush = false;
				}
			}
			this.selected = selected;
			return (selected != null) ;
		}

		@Override
		public void mouseClicked(MouseEvent e) {
			System.out.println("clicked");
//			if (this.event_publisher != null) {
//				std_msgs.String msg = this.event_publisher.newMessage();
//				msg.setData("clicked");
//				this.event_publisher.publish(msg);
//			}
			if ( updateSelectedImage(e.getX(), e.getY())){
				System.out.println(" selected -> " + this.selected);
				this.selected.x = e.getX() - this.selected.w/2;
				this.selected.y = e.getY() - this.selected.h/2;
			}
			repaint();
		}

		@Override
		public void mouseEntered(MouseEvent arg0) {
		}

		@Override
		public void mouseExited(MouseEvent arg0) {
		}

		@Override
		public void mousePressed(MouseEvent arg0) {
		}

		@Override
		public void mouseReleased(MouseEvent arg0) {
		}

		@Override
		public void mouseDragged(MouseEvent e) {
			if ( updateSelectedImage(e.getX(), e.getY())){
				System.out.println(" drag selected -> " + this.selected);
				this.selected.x = e.getX() - this.selected.w/2;
				this.selected.y = e.getY() - this.selected.h/2;
			}
			repaint();
//			if (this.selected_movie != null && this.selected_movie.selected ) {
//				switch (this.mode) {
//				case MouseEvent.BUTTON1:
//					this.selected_movie.x += e.getX() - this.lx ;
//					this.selected_movie.y += e.getY() - this.ly ;
//					this.selected_movie.update_values() ;
//					this.lx = e.getX();
//					this.ly = e.getY();
//					break;
//				case MouseEvent.BUTTON2:
//					this.selected_movie.inW += e.getX() - this.mx ;
//					this.selected_movie.inH += e.getY() - this.my ;
//					this.selected_movie.update_values() ;
//					this.mx = e.getX();
//					this.my = e.getY();
//					break;
//				case MouseEvent.BUTTON3:
//					this.selected_movie.width += e.getX() - this.rx ;
//					this.selected_movie.height += e.getY() - this.ry ;
//					this.selected_movie.inW = this.selected_movie.width ;
//					this.selected_movie.inH = this.selected_movie.height ;
//					this.selected_movie.update_values() ;
//					this.rx = e.getX();
//					this.ry = e.getY();
//					break;
//				}
//			}
		}

		@Override
		public void mouseMoved(MouseEvent arg0) {
		}

	}

	
}
