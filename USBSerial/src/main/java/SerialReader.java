import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class SerialReader {

	private CommPortIdentifier pi;
	private CommPort cp;
	private InputStream in;
	private OutputStream out;
	private boolean running;
	private int bit;

	public SerialReader(String id, int rate) {
		try {
			System.out.println("SerialReader: " + id + "(" + rate + ")");
			this.pi = CommPortIdentifier.getPortIdentifier(id);
			this.cp = this.pi.open(id, rate);
			SerialPort sp = (SerialPort) this.cp;
			sp.setSerialPortParams(rate, SerialPort.DATABITS_8,
					SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
			sp.setDTR(true);
			sp.setRTS(false);
			this.in = sp.getInputStream();
			this.out = sp.getOutputStream();
			this.running = true;
			this.bit = 0;
			System.out.println(" --- intialized");
		} catch (NoSuchPortException e) {
			e.printStackTrace();
		} catch (PortInUseException e) {
			e.printStackTrace();
		} catch (UnsupportedCommOperationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void putBit(int bit) {
		try {
			synchronized (this.out) {
				this.bit = bit;
				this.out.write(bit);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public int getBit(){
		synchronized (this.out) {
			return this.bit;
		}
	}
	
	@Override
	protected void finalize() throws Throwable {
		try {
			super.finalize();
		} finally {
			this._finalize();
		}
	}

	public void _finalize() throws IOException {
		this.running = false;
		if (this.cp != null) {
			this.cp.close();
			this.cp = null;
		}
		synchronized (this.in) {
			if (this.in != null) {
				this.in.close();
				this.in = null;
			}
		}
		if (this.out != null) {
			this.out.close();
			this.out = null;
		}
	}

	public void write(String com) throws IOException {
		for (char c : com.toCharArray()) {
			this.out.write(c);
		}
		this.out.write('\n');
		// this.out.flush() ;
	}

	public String readLine() throws IOException {
		return this.readLine(this.in);
	}

	private String readLine(InputStream in) throws IOException {
		StringBuilder buf = new StringBuilder();
		int tmp;
		while (true) {
			tmp = in.read();
			if (tmp <= 0 || tmp == '\n') {
				// System.out.println( "break with " + tmp ) ;
				break;
			}
			buf.append((char) tmp);
			// System.out.println(buf.toString()) ;
		}
		return buf.toString();
	}

	public static void main(String[] args) {
		final SerialReader t1 = new SerialReader("/dev/ttyUSB0", 9600);
		// final SerialReader t2 = new SerialReader("/dev/ttyUSB1", 9600);
		//
		new Thread(new Runnable() {
			@Override
			public void run() {
				while (t1.running) {
					try {
						synchronized (t1.in) {
							if (t1.in != null)
								System.out.println(t1.getBit() + " -- " + t1.in.read());
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}).start();
		//
		byte[] buf = new byte[1];
		int ret;
		while (true) {
			try {
				t1.putBit(0x00);
				Thread.sleep(0, 200);
				t1.putBit(0x01);
				Thread.sleep(0, 200);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		// ti._finalize() ;
	}

}
