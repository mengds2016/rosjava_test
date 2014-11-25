package com.github.rosjava_test.rosjava_image_util;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.ros.node.topic.Publisher;

public class ImageWindowSampleFrame extends JFrame {
	private static final long serialVersionUID = 1L;

	private static int N = 3;
	private static int W = 480*N, H = 640;

	private BorderLayout camera_layout;
	private BorderLayout outer_layout;
	private JPanel camera_pane;

	public ImageView leftCameraView, centerCameraView, rightCameraView;
	public CommandView commandView;

	public Publisher<std_msgs.String> file_receive_pub;

	public ImageWindowSampleFrame() {
		this.camera_layout = new BorderLayout();
		this.outer_layout = new BorderLayout();
		this.camera_pane = new JPanel();
		this.camera_pane.setLayout(this.camera_layout);
		this.getContentPane().setLayout(this.outer_layout);

		this.commandView = new CommandView();
		this.leftCameraView = new ImageView(this.commandView, (W-4)/N, H-20);
		this.rightCameraView = new ImageView(this.commandView, (W-4)/N, H-20);
		this.centerCameraView = new ImageView(this.commandView, (W-4)/N, H-20);

		this.camera_pane.add(this.leftCameraView, BorderLayout.WEST);
		this.camera_pane.add(this.centerCameraView, BorderLayout.CENTER);
		this.camera_pane.add(this.rightCameraView, BorderLayout.EAST);

		try {
			String home_dir = System.getenv("IMG_HOME"); 
			if ( home_dir == null ){
				home_dir = System.getenv("HOME") + "/prog/euslib/demo/s-noda/tmp-ros-package/rosjava_test/rosjava_image_util/img";
			}
			//"/home/s-noda/prog/euslib/demo/s-noda/tmp-ros-package/rosjava_test/rosjava_image_util/img"
			BufferedImage elbow1 = ImageIO.read(new File(home_dir+"/elbow.png"));
			BufferedImage elbow2 = ImageIO.read(new File(home_dir+"/elbow.png"));
			BufferedImage hand1 = ImageIO.read(new File(home_dir+"/hand.png"));
			BufferedImage hand2 = ImageIO.read(new File(home_dir+"/hand.png"));
			BufferedImage pelvis = ImageIO.read(new File(home_dir+"/pelvis.png"));
			this.leftCameraView.pane.addImage("left_elbow", elbow1, W/(N*4), H/2, -1, -1);
			this.leftCameraView.pane.addImage("right_elbow", elbow2, 3*W/(N*4), H/2, -1, -1);
			this.leftCameraView.pane.addImage("left_hand", hand1, W/(N*4), 3*H/4, -1, -1);
			this.leftCameraView.pane.addImage("right_hand", hand2, 3*W/(N*4), 3*H/4, -1, -1);
			this.leftCameraView.pane.addImage("pelvis", pelvis, 2*W/(N*4), 2*H/4, -1, -1);
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
		
		DropTarget target = new DropTarget(this.camera_pane, new DropAdapterUtil() {
			@Override
			public void drop(File[] files) {
				for (File f : files) {
					if ( ImageWindowSampleFrame.this.file_receive_pub != null ){
						std_msgs.String msg = ImageWindowSampleFrame.this.file_receive_pub.newMessage();
						msg.setData(f.getAbsolutePath());
						ImageWindowSampleFrame.this.file_receive_pub.publish(msg);
					}
				}
			}
		});
	}

	public void setLeftImage(BufferedImage i){
		this.leftCameraView.pane.setBgImage(i);
		this.repaint();
	}
	
	public void setRightImage(BufferedImage i){
		this.rightCameraView.pane.setBgImage(i);
		this.repaint();
	}
	
	public ArrayList<ImageData> getImageDataList(){
		return this.leftCameraView.pane.getImageList();
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
		public BufferedImage overlayImage;
		public double alpha = 0.5;
		public String name;
		public int x=0, y=0, w=100, h=100;	
		public boolean flush=false;
		public Publisher<std_msgs.Int32MultiArray> rect_publisher;
		public Publisher<std_msgs.Float32MultiArray> rect_normal_publisher;
		public float scale_factor=1.0f;

		public void setImage(BufferedImage i) {
			this.image = i;
			this.w = this.image.getWidth();
			this.h = this.image.getHeight();
		}

		public BufferedImage getImage() {
			return this.image;
		}
		
		public void setOverlayImage(BufferedImage i, double alpha){
			this.alpha = alpha;
			// this.overlayImage = i;
			BufferedImage buf = new BufferedImage(i.getWidth(), i.getHeight(), BufferedImage.TYPE_INT_ARGB);
			for ( int w=0 ; w<buf.getWidth(); w++ ){
				for ( int h=0 ; h<buf.getHeight(); h++ ){
					int argb = i.getRGB(w, h);
					argb = (((int)(255 * this.alpha)) << 24) | (argb & 0x00ffffff);
					buf.setRGB(w, h, argb);
				}
			}
			this.overlayImage = buf;
		}
		
		public void clickUpdate(int x, int y, int w, int h, float scale){
			this.x = x - this.w/2 ;
			this.y = y - this.h/2;
			if ( this.rect_publisher != null ){
				std_msgs.Int32MultiArray msg = this.rect_publisher.newMessage();
				msg.setData(new int[]{this.x, this.y, this.w, this.h});
				this.rect_publisher.publish(msg);
			}
			if ( this.rect_normal_publisher != null ){
				std_msgs.Float32MultiArray msg = this.rect_normal_publisher.newMessage();
				//float scale = 1.0f / w ;
				msg.setData(new float[]{this.x * scale, this.y * scale, this.w * scale, this.h * scale});
				this.rect_normal_publisher.publish(msg);
			}
		}
		
		public void drawBackground(Graphics g, int panel_w, int panel_h) {
			BufferedImage i;
			i = this.image;
			if (i != null) {
				double rate = Math.min(1.0 * panel_w / this.w, 1.0 * panel_h
						/ this.h);
				double woffset = (panel_w - this.w * rate) / 2;
				double hoffset = (panel_h - this.h * rate) / 2;
				g.drawImage(i, (int) (woffset), (int) (hoffset),
						(int) (panel_w - woffset * 2),
						(int) (panel_h - hoffset * 2), null);
				this.scale_factor = (float)(1.0 / (panel_w - woffset * 2));
			} else {
				g.clearRect(0, 0, panel_w, panel_h);
				g.drawString("NO IMAGE", panel_w / 2, panel_h / 2);
			}
			//
			i = this.overlayImage;
			if (i != null) {
//				BufferedImage buf = new BufferedImage(i.getWidth(), i.getHeight(), BufferedImage.TYPE_INT_ARGB);
//				for ( int w=0 ; w<buf.getWidth(); w++ ){
//					for ( int h=0 ; h<buf.getHeight(); h++ ){
//						int argb = i.getRGB(w, h);
//						argb = (((int)(255 * this.alpha)) << 24) | (argb & 0x00ffffff);
//						buf.setRGB(w, h, argb);
//					}
//				}
//				i = buf;
				double rate = Math.min(1.0 * panel_w / i.getWidth(), 1.0 * panel_h
						/ i.getHeight());
				double woffset = (panel_w - i.getWidth() * rate) / 2;
				double hoffset = (panel_h - i.getHeight() * rate) / 2;
				g.drawImage(i, (int) (woffset), (int) (hoffset),
						(int) (panel_w - woffset * 2),
						(int) (panel_h - hoffset * 2), null);
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
		
		public ArrayList<ImageData> getImageList(){
			return this.images;
		}

		public void setBgImage(BufferedImage i) {
			this.bgImage.setImage(i);
		}

		public BufferedImage getBgImage() {
			return this.bgImage.getImage();
		}
		
		public void setOverlayImage(BufferedImage i){
			//this.bgImage.overlayImage = i;
			this.bgImage.setOverlayImage(i, 0.5);
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
		final public ImagePanel pane;
		public int w, h;
		private JLabel prompt;
		
		private ImageData selected;

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
			if ( updateSelectedImage(e.getX(), e.getY())){
				System.out.println(" selected -> " + this.selected);
				this.selected.clickUpdate(e.getX(), e.getY(), this.pane.getWidth(), this.pane.getHeight(), this.pane.bgImage.scale_factor);
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
				this.selected.clickUpdate(e.getX(), e.getY(), this.pane.getWidth(), this.pane.getHeight(), this.pane.bgImage.scale_factor);
			}
			repaint();
//				switch (this.mode) {
//				case MouseEvent.BUTTON1:
//				case MouseEvent.BUTTON2:
//				case MouseEvent.BUTTON3:
		}

		@Override
		public void mouseMoved(MouseEvent arg0) {
		}

	}
	
	public abstract class DropAdapterUtil extends DropTargetAdapter{
		public abstract void drop( File[] files ) ;
		@Override
		public void drop(DropTargetDropEvent e) {
			File[] files = new File[0];
			try {
				Transferable transfer = e.getTransferable();
				if (transfer
						.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
					// Windows
					e.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
					files = (File[]) ((List<File>) (transfer
							.getTransferData(DataFlavor.javaFileListFlavor)))
							.toArray();
				} else if (transfer
						.isDataFlavorSupported(DataFlavor.stringFlavor)) {
					// Linux
					if (e.isLocalTransfer()) {
					} else {
						e.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
						String str = (String) transfer
								.getTransferData(DataFlavor.stringFlavor);
						String lineSep = System
								.getProperty("line.separator");
						String[] fileList = str.split(lineSep);
						files= new File[fileList.length];
						for (int i = 0; i < files.length; i++) {
							URI fileURI = new URI(fileList[i].replaceAll("[\r\n]", ""));
							files[i] = new File(fileURI);
						}
					}
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
//			for ( File f :files ){
//				System.out.println( f.getName() ) ;
//			}
			drop( files ) ;
		}
	}
}
