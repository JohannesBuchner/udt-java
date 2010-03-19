package udt;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

import udt.packets.Destination;
import udt.packets.Shutdown;
import udt.util.UDTStatistics;

public class UDTClient {
	
	private static final Logger logger=Logger.getLogger(UDTClient.class.getName());
	private final UDPEndPoint clientEndpoint;
    private ClientSession clientSession;
    
   
    //construstor
    public UDTClient(InetAddress address, int localport)throws SocketException, UnknownHostException{
    	//create endpoint
    	clientEndpoint=new UDPEndPoint(address,localport);
    	logger.info("Created client endpoint on port "+localport);
	}
    
    //constructor
    public UDTClient(UDPEndPoint endpoint)throws SocketException, UnknownHostException{
    	clientEndpoint=endpoint;
    }

    /**
     * establishes a connection to the given server. 
     * Starts the sender thread.
     * @param host
     * @param port
     * @throws UnknownHostException
     */
   public void connect(String host, int port)throws InterruptedException, UnknownHostException, IOException{
		//create client session...
		clientSession=new ClientSession(clientEndpoint);
		Destination destination=new Destination(host,port);
		clientSession.setDestination(destination);
		clientEndpoint.addSession(0L, clientSession);
		clientEndpoint.addDestination(0L, destination);
		
		clientEndpoint.start();
		clientSession.connect();
		//wait for handshake
		while(!clientSession.isReady()){
			Thread.sleep(500);
		}
		logger.info("The UDTClient is connected");
		Thread.sleep(500);
	}

	/**
	 * sends the given data asynchronously
	 * 
	 * @param data
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void send(byte[]data)throws IOException, InterruptedException{
		clientSession.getSocket().doWrite(data);
	}
	
	public void sendBlocking(byte[]data)throws IOException, InterruptedException{
		clientSession.getSocket().doWriteBlocking(data);
	}
	
	public int read(byte[]data)throws IOException, InterruptedException{
		return clientSession.getSocket().getInputStream().read(data);
	}
	
	/**
	 * flush outstanding data (and make sure it is acknowledged)
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void flush()throws IOException, InterruptedException{
		clientSession.getSocket().flush();
	}
	
	
	public void shutdown()throws IOException{
		
		if (clientSession.isReady()&& clientSession.active==true) 
		{
			Shutdown shutdown = new Shutdown();
			shutdown.setDestinationID(0l);//TODO
			try{
				clientEndpoint.doSend(shutdown);
			}
			catch(IOException e)
			{
				logger.log(Level.SEVERE,"ERROR: Connection could not be stopped!",e);
			}
			clientSession.getSocket().getReceiver().stop();
			clientEndpoint.stop();
		}
	}
	
	public UDTInputStream getInputStream()throws IOException{
		return clientSession.getSocket().getInputStream();
	}
	
	public UDTOutputStream getOutputStream()throws IOException{
		return clientSession.getSocket().getOutputStream();
	}
	
	public UDPEndPoint getEndpoint()throws IOException{
		return clientEndpoint;
	}
	
	public UDTStatistics getStatistics(){
		return clientSession.getStatistics();
	}
	
}
