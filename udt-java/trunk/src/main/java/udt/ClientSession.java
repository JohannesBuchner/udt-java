/*********************************************************************************
 * Copyright (c) 2010 Forschungszentrum Juelich GmbH 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * (1) Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the disclaimer at the end. Redistributions in
 * binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution.
 * 
 * (2) Neither the name of Forschungszentrum Juelich GmbH nor the names of its 
 * contributors may be used to endorse or promote products derived from this 
 * software without specific prior written permission.
 * 
 * DISCLAIMER
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *********************************************************************************/

package udt;

import java.io.IOException;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;

import udt.packets.ConnectionHandshake;
import udt.packets.Destination;
import udt.packets.Shutdown;
import udt.util.SequenceNumber;

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
	
	public ClientSession(UDPEndPoint endPoint, Destination dest)throws SocketException{
		super("ClientSession localPort="+endPoint.getLocalPort(),dest);
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
		sendHandShake();
		while(getState()!=ready){
			if(getState()==invalid)throw new IOException("Can't connect!");
			n++;
			if(getState()!=ready)Thread.sleep(500);
		}
		cc.init();
		logger.info("Connected, "+n+" handshake packets sent");		
	}
	
	@Override
	public void received(UDTPacket packet, Destination peer) {
		
		lastPacket=packet;
		
		if (getState()!=ready && packet instanceof ConnectionHandshake) {
			try{
				logger.info("Received connection handshake from "+peer);
				//TODO validate parameters sent by peer
				setState(ready);
				long peerSocketID=((ConnectionHandshake)packet).getSocketID();
				destination.setSocketID(peerSocketID);
				socket=new UDTSocket(endPoint,this);
			}catch(Exception ex){
				logger.log(Level.WARNING,"Error creating socket",ex);
				setState(invalid);
			}
			return;
		}
		if(getState() == ready) {
			
			if(packet instanceof Shutdown){
				setState(shutdown);
				active=false;
				logger.info("Connection shutdown initiated by the other side.");
				return;
			}
			active = true;
			try{
				if(packet.forSender()){
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
		ConnectionHandshake handshake = new ConnectionHandshake();
		handshake.setConnectionType(1);
		handshake.setSocketType(1);
		long initialSequenceNo=SequenceNumber.random();
		setInitialSequenceNumber(initialSequenceNo);
		handshake.setInitialSeqNo(initialSequenceNo);
		handshake.setPacketSize(getDatagramSize());
		handshake.setSocketID(mySocketID);
		handshake.setSession(this);
		logger.info("Handshake to "+this.getDestination());
		endPoint.doSend(handshake);
	}
	
	

	public UDTPacket getLastPkt(){
		return lastPacket;
	}


}
