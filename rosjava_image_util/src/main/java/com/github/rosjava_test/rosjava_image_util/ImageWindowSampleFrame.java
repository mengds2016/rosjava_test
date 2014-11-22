package com.github.rosjava_test.rosjava_image_util;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.ros.node.topic.Publisher;

public class ImageWindowSampleFrame extends JFrame implements ActionListener {
	private static final long serialVersionUID = 1L;

	private static int W = 480*2, H = 640;

	private BorderLayout camera_layout;
	private BorderLayout outer_layout;
	private JPanel camera_pane;

	public CameraView leftCameraView, rightCameraView;
	public CommandView commandView;

	private float pan = 0;
	private float tlt = 0;
	private Publisher<std_msgs.Float32MultiArray> command_publisher;
	private Publisher<std_msgs.String> event_publisher;

	public ImageWindowSampleFrame() {
		this.camera_layout = new BorderLayout();
		this.outer_layout = new BorderLayout();
		this.camera_pane = new JPanel();
		this.camera_pane.setLayout(this.camera_layout);
		this.getContentPane().setLayout(this.outer_layout);

		this.commandView = new CommandView();
		this.leftCameraView = new CameraView(this.commandView);
		this.rightCameraView = new CameraView(this.commandView);

		this.camera_pane.add(this.leftCameraView, BorderLayout.CENTER);
		this.camera_pane.add(this.rightCameraView, BorderLayout.EAST);

//		Dimension min = new Dimension(W, 50);
//		JButton west = new JButton();
//		west.setActionCommand("west");
//		west.setName("west");
//		west.addActionListener(this);
//		west.setText("◀");
//		// west.setPreferredSize(min);
//		this.camera_pane.add(west, BorderLayout.WEST);
//
//		JButton east = new JButton();
//		east.setActionCommand("east");
//		east.setName("east");
//		east.setText("▶");
//		east.addActionListener(this);
//		// east.setPreferredSize(min);
//		this.camera_pane.add(east, BorderLayout.EAST);
//
//		JButton north = new JButton();
//		north.setActionCommand("north");
//		north.setName("north");
//		north.addActionListener(this);
//		north.setPreferredSize(min);
//		north.setText("▲");
//		this.camera_pane.add(north, BorderLayout.NORTH);
//
//		JButton south = new JButton();
//		south.setActionCommand("south");
//		south.setName("south");
//		south.addActionListener(this);
//		south.setPreferredSize(min);
//		south.setText("▼");
//		this.camera_pane.add(south, BorderLayout.SOUTH);

		this.add(this.camera_pane);
		this.add(this.commandView, BorderLayout.SOUTH);

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setTitle("KubiInterface");
		// this.setPreferredSize(new Dimension(W,H)) ;
		setSize(W, H);

		setBackground(Color.black);

		setVisible(true);
	}

	public void setCommandPublisher(
			Publisher<std_msgs.Float32MultiArray> command) {
		this.command_publisher = command;
		std_msgs.Float32MultiArray com = this.command_publisher.newMessage();
		com.setData(new float[] { this.pan, this.tlt });
		this.command_publisher.publish(com);
	}

	public void setEventPublisher(Publisher<std_msgs.String> event) {
		this.event_publisher = event;
	}
	
	public void setLeftImage(BufferedImage i){
		this.leftCameraView.camera.setImage(i);
		this.repaint();
	}
	
	public void setRightImage(BufferedImage i){
		this.rightCameraView.camera.setImage(i);
		this.repaint();
	}

	public static void main(String[] args) {
		new ImageWindowSampleFrame().repaint();
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		String cm = arg0.getActionCommand();
		if (cm.contentEquals("north")) {
			tlt += 5;
		} else if (cm.contains("south")) {
			tlt -= 5;
		} else if (cm.contains("east")) {
			pan -= 5;
		} else if (cm.contains("west")) {
			pan += 5;
		}
		if (this.command_publisher != null) {
			std_msgs.Float32MultiArray com = this.command_publisher
					.newMessage();
			com.setData(new float[] { this.pan, this.tlt });
			this.command_publisher.publish(com);
		}
		if (this.event_publisher != null) {
			std_msgs.String msg = this.event_publisher.newMessage();
			msg.setData(cm);
			this.event_publisher.publish(msg);
		}
	}
	
	
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
	
	public class CameraView extends JPanel implements MouseListener {
		private static final long serialVersionUID = 3L;
		
		private GridLayout out;
		//final public String topic_raw, topic_compressed;
		final public ImagePanel camera;
		public int w, h;

		private JLabel prompt;
		private Publisher<std_msgs.String> event_publisher;

		public CameraView(JLabel prompt) {
			this.prompt = prompt;
			this.out = new GridLayout(1, 1);
			this.camera = new ImagePanel();
			this.setLayout(this.out);
			this.add(this.camera);
			this.addMouseListener(this);
			setVisible(true);
		}
		
		public void setEventPublisher(Publisher<std_msgs.String> event) {
			this.event_publisher = event;
		}
		
		public class ImagePanel extends JPanel {
			private static final long serialVersionUID = 8L;

			private BufferedImage image;
			private BufferedImage red_image;
			private int filter ;
			private int w, h;

			public ImagePanel(){
				this(600,600);
			}
			
			public ImagePanel(int w, int h) {
				this.w = w ;
				this.h = h ;
				this.setPreferredSize(new Dimension(w, h));
			}

			public void setImage(BufferedImage i) {
				this.image = i;
				if ( this.red_image != null ){
					red_filter(this.filter) ;
					repaint() ;
				}
			}

			public BufferedImage getImage() {
				return this.image;
			}
			
			public void emergency ( final long time, int col ){
				red_filter(col) ;
				this.repaint() ;
				new Thread( new Runnable(){
					@Override
					public void run() {
						try {
							Thread.sleep(time) ;
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						ImagePanel.this.red_image = null ;
						ImagePanel.this.repaint() ;
					}
				} ).start() ;
			}

			public void red_filter(int col) {
				if (this.image == null) {
					return;
				}
				if (this.red_image != null && col == this.filter) {
					return;
				}
				this.filter = col ;
				this.red_image = new BufferedImage(this.image.getWidth(),
						this.image.getHeight(), this.image.getType());
				for (int x = 0; x < this.image.getWidth(); x++) {
					for (int y = 0; y < this.image.getHeight(); y++) {
						this.red_image.setRGB(x, y,
								col & this.image.getRGB(x, y));
					}
				}
			}

			@Override
			public void paintComponent(Graphics g) {
				BufferedImage i = this.image ;
				if ( this.red_image != null ){
					i = this.red_image ;
				}
				if (i != null) {
					this.w = this.getWidth();
					this.h = this.getHeight();
					int w = i.getWidth();
					int h = i.getHeight();
					double rate = Math.min(1.0 * this.w / w, 1.0 * this.h / h);
					double woffset = (this.w - w * rate) / 2;
					double hoffset = (this.h - h * rate) / 2;
					g.drawImage(i, (int) (woffset), (int) (hoffset),
							(int) (this.w - woffset * 2),
							(int) (this.h - hoffset * 2), null);
				} else {
					g.clearRect(0, 0, w, h);
					g.drawString("NO IMAGE", w / 2, h / 2);
				}
			}

			@Override
			public void paint(Graphics g) {
				paintComponent(g);
			}
		}

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

	}

	
}
