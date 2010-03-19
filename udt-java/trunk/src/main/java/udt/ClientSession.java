package udt;

import java.io.IOException;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;

import udt.packets.ConnectionHandshake;
import udt.packets.Destination;
import udt.packets.Shutdown;

/**
 * Keep state of a UDT connection. Once established, the 
 * session provides a valid {@link UDTSocket}.
 * This can be used as client session in both client-server mode and rendezvous mode.
 * 
 * 
 */
public class ClientSession extends UDTSession {

	private static final Logger logger=Logger.getLogger(ClientSession.class.getName());

	private UDPEndPoint endPoint;
	
	public ClientSession(UDPEndPoint endPoint)throws SocketException{
		super("ClientSession localPort="+endPoint.getLocalPort());
		this.endPoint=endPoint;
		logger.info("Created "+toString());
	}

	/**
	 * send connection handshake until a reply from server is received
	 * TODO check for timeout
	 * @throws InterruptedException
	 * @throws IOException
	 */
	
	public void connect() throws InterruptedException,IOException{
		int n=0;
		while(getState()!=ready){
			if(getState()==invalid)throw new IOException("Can't connect!");
			sendHandShake();
			n++;
			if(getState()!=ready)Thread.sleep(500);
		}
		logger.info("Connected, "+n+" handshake packets sent");		
	}


	int n_dataPacket = 0;
	int n_keepAlivePacket=0;
	int n_shutdownPacket=0;

	@Override
	public void received(UDTPacket p, Destination peer) {
		
		lastPacket=p;
		
		if (getState()!=ready && lastPacket instanceof ConnectionHandshake) {
			try{
				logger.info("Creating UDTSocket for client session "+toString());
				setState(ready);
				socket=new UDTSocket(endPoint,this);
			}catch(Exception ex){
				logger.log(Level.WARNING,"Error creating socket",ex);
				setState(invalid);
			}
			return;
		}
		if(getState() == ready) {
			if(lastPacket instanceof Shutdown){
				setState(shutdown);
				active=false;
				logger.info("Connection shutdown initiated by the other side.");
				return;
			}
			active = true;
			try{
				//packet received means we should not yet expire
				socket.getReceiver().resetEXPTimer();
				if(lastPacket.forSender()){
					socket.getSender().receive(lastPacket);
				}else{
					socket.getReceiver().receive(lastPacket);	
				}
			}catch(Exception ex){
				//session is invalid
				logger.log(Level.SEVERE,"Error in "+toString(),ex);
				setState(invalid);
			}
			return;
		 }
	}


	//handshake for connect
	protected void sendHandShake()throws IOException{
		ConnectionHandshake reqHandshakePkt = new ConnectionHandshake();
		reqHandshakePkt.setConnectionType(1);
		reqHandshakePkt.setSocketType(1);
		reqHandshakePkt.setInitialSeqNo(1);
		reqHandshakePkt.setPacketSize(getDatagramSize());
		reqHandshakePkt.setDestinationID(0);
		endPoint.doSend(reqHandshakePkt);
	}
	
	

	public UDTPacket getLastPkt(){
		return lastPacket;
	}


}
