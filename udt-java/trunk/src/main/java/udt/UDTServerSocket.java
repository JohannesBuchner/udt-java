package udt;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;



public class UDTServerSocket {
	private static final Logger logger=Logger.getLogger(UDTClient.class.getName());
	
	private final UDPEndPoint endpoint;
	
	private boolean started=false;
	
	private volatile boolean shutdown=false;
	
	/**
	 * create a UDT ServerSocket
	 * @param localAddress
	 * @param port - the local port. If 0, an ephemeral port will be chosen
	 */
	public UDTServerSocket(InetAddress localAddress, int port)throws SocketException,UnknownHostException{
		endpoint=new UDPEndPoint(localAddress,port);
		logger.info("Created server endpoint on port "+endpoint.getLocalPort());
	}

	//starts a server on localhost
	public UDTServerSocket(int port)throws SocketException,UnknownHostException{
		this(InetAddress.getLocalHost(),port);
	}
	
	/**
	 * listens and blocks until a new client connects and returns a valid {@link UDTSocket}
	 * for the new connection
	 * @return
	 */
	public synchronized UDTSocket accept()throws InterruptedException{
		if(!started){
			endpoint.start(true);
			started=true;
		}
		while(!shutdown){
			UDTSession session=endpoint.accept(10000, TimeUnit.MILLISECONDS);
			if(session!=null){
				//wait for handshake to complete
				while(!session.isReady() || session.getSocket()==null){
					Thread.sleep(100);
				}
				return session.getSocket();
			}
		}
		throw new InterruptedException();
	} 
	
	public void shutDown(){
		shutdown=true;
		endpoint.stop();
	}
	
	public UDPEndPoint getEndpoint(){
		return endpoint;
	}
}
