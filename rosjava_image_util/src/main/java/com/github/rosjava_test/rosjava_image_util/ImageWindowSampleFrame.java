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
import java.util.ArrayList;

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
		this.leftCameraView.camera.setBgImage(i);
		this.repaint();
	}
	
	public void setRightImage(BufferedImage i){
		this.rightCameraView.camera.setBgImage(i);
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
		private int x=0, y=0, w=100, h=100;	
		
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
		final public ImagePanel camera;
		public int w, h;

		private JLabel prompt;
		private Publisher<std_msgs.String> event_publisher;

		public ImageView(JLabel prompt, int w, int h) {
			this.prompt = prompt;
			this.out = new GridLayout(1, 1);
			this.camera = new ImagePanel(w, h);
			this.setLayout(this.out);
			this.add(this.camera);
			this.addMouseListener(this);
			setVisible(true);
		}
		
		public void setEventPublisher(Publisher<std_msgs.String> event) {
			this.event_publisher = event;
		}
		
		
//		public boolean update_selected_movie(int x, int y) {
//			for (int i = MainFrame.movie.size()-1 ; i>=0 ; i--  ) {
//				Movie mov = MainFrame.movie.get(i) ;
//				if (x > mov.x && x < mov.x + mov.width && y > mov.y
//						&& y < mov.y + mov.height) {
//					this.selected_movie = mov;
//					this.selected_movie.selected = true;
//					//System.out.println( "selected" ) ;
//					return true ;
//				}
//				mov.selected = false ;
//			}
//			return false ;
//		}

		@Override
		public void mouseClicked(MouseEvent arg0) {
			System.out.println("clicked");
			if (this.event_publisher != null) {
				std_msgs.String msg = this.event_publisher.newMessage();
				msg.setData("clicked");
				this.event_publisher.publish(msg);
			}
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
		public void mouseDragged(MouseEvent arg0) {
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
