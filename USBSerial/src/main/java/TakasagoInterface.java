import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


public class TakasagoInterface {

	private CommPortIdentifier pi;
	private CommPort cp ;
	private InputStream in ;
	private OutputStream out ;
	
	public TakasagoInterface(String id, int rate){
		try {
			this.pi = CommPortIdentifier.getPortIdentifier(id);
			this.cp = this.pi.open("TakasagoInterface", rate);
            SerialPort sp = (SerialPort) this.cp;
            sp.setSerialPortParams(
                    9600, 
                    SerialPort.DATABITS_8, 
                    SerialPort.STOPBITS_1, 
                    SerialPort.PARITY_NONE);
            sp.setDTR(true);
            sp.setRTS(false);
            this.in = sp.getInputStream() ;
            this.out = sp.getOutputStream() ;

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
	
	@Override
	protected void finalize() throws Throwable {
		try {
			super.finalize();
		} finally {
			this._finalize() ;
		}
	}
	
	public void _finalize() throws IOException{
		if ( this.cp != null ){
			this.cp.close() ;
			this.cp = null ;
		}
		if ( this.in != null ){
			this.in.close() ;
			this.in = null ;
		}
		if ( this.out != null ){
			this.out.close() ;
			this.out = null ;
		}
	}
	
	public void write( String com ) throws IOException{
		for ( char c : com.toCharArray() ){
			this.out.write(c) ;
		}
		this.out.write('\n'); 
		//this.out.flush() ;
	}

	public String readLine() throws IOException{
		return this.readLine(this.in) ;
	}
	
	private String readLine(InputStream in) throws IOException{
		StringBuilder buf = new StringBuilder() ;
		int tmp ;
		while ( true ) {
			tmp = in.read() ;
			if ( tmp <= 0 || tmp == '\n'){
				//System.out.println( "break with " + tmp ) ;
				break ;
			}
			buf.append((char)tmp) ;
			//System.out.println(buf.toString()) ;
		}
		return buf.toString() ;
	}

   public static void main(String[] args) throws IOException, NoSuchPortException, PortInUseException, UnsupportedCommOperationException {
	   TakasagoInterface ti = new TakasagoInterface("/dev/ttyUSB0", 9600) ;
	   while (true){
		   String command = ti.readLine(System.in) ;
		   if ( command.contentEquals("exit") ){
			   break ;
		   }
		   ti.write(command) ;
		   System.out.println(ti.readLine()) ;
	   }
	   ti._finalize() ;
   }
   
}
