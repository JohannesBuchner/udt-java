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
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

import udt.packets.ConnectionHandshake;
import udt.packets.Destination;
import udt.packets.KeepAlive;
import udt.packets.Shutdown;
import udt.util.Util;

/**
 * server side session in client-server mode
 */
public class ServerSession extends UDTSession {

	private static final Logger logger=Logger.getLogger(ServerSession.class.getName());

	private final InetAddress peer;
	private final int port;
	private final UDPEndPoint endPoint;

	//last received packet
	private UDTPacket lastPacket;

	//time we last received a packet from the other side. Needed for connection timeout
	private long lastPacketReceivedTime;

	public ServerSession(DatagramPacket dp,UDPEndPoint endPoint)throws SocketException,UnknownHostException{
		super("ServerSession localPort="+endPoint.getLocalPort()+" peer="+dp.getAddress()+":"+dp.getPort());
		this.endPoint=endPoint;
		this.peer=dp.getAddress();
		this.port=dp.getPort();
		setDestination(new Destination(peer,port));
		logger.info("Created "+toString()+" talking to "+getDestination());
	}

	int n_handshake=0;

	@Override
	public void received(UDTPacket p, Destination peer){
		lastPacket=p;
		lastPacketReceivedTime=Util.getCurrentTime();

		if (getState()<=ready && lastPacket instanceof ConnectionHandshake) {
			logger.info("Received ConnectionHandshake from "+peer);

			if(getState()<=handshaking){
				setState(handshaking);
				this.endPoint.addDestination(0L, peer);
			}
			try{
				sendResponseHandShake(peer);
				n_handshake++;
				try{
					setState(ready);
					socket=new UDTSocket(endPoint, this);
				}catch(Exception uhe){
					//session is invalid
					logger.log(Level.SEVERE,"",uhe);
					setState(invalid);
				}
			}catch(IOException ex){
				//session invalid
				logger.log(Level.WARNING,"Error processing ConnectionHandshake",ex);
				setState(invalid);
			}
			return;
		}else if(lastPacket instanceof KeepAlive) {
			socket.getReceiver().resetEXPTimer();
			active = true;
			return;
		}

		if(getState()== ready) {
			active = true;

			if (lastPacket instanceof KeepAlive) {
				//nothing to do here
				return;
			}else if (lastPacket instanceof Shutdown) {
				try{
					socket.getReceiver().stop();
				}catch(IOException ex){
					logger.log(Level.WARNING,"",ex);
				}
				setState(shutdown);
				System.out.println("SHUTDOWN ***");
				active = false;
				logger.info("Connection shutdown initiated by the other side.");
				return;
			}

			else{
				try{
					if(lastPacket.forSender()){
						socket.getSender().receive(lastPacket);
					}else{
						socket.getReceiver().receive(lastPacket);	
					}
				}catch(Exception ex){
					//session invalid
					logger.log(Level.SEVERE,"",ex);
					setState(invalid);
				}
			}
			return;

		}


	}

	/**
	 * for testing use only
	 */
	public UDTPacket getLastPkt(){
		return lastPacket;
	}

	protected void sendResponseHandShake(Destination peer)throws IOException{
		ConnectionHandshake respHandshakePkt = new ConnectionHandshake();
		//compare the packet size and choose minimun
		long clientBufferSize=((ConnectionHandshake)lastPacket).getPacketSize();
		long myBufferSize=getDatagramSize();
		long bufferSize=Math.min(clientBufferSize, myBufferSize);
		setDatagramSize((int)bufferSize);
		respHandshakePkt.setPacketSize(bufferSize);
		respHandshakePkt.setUdtVersion(4);
		respHandshakePkt.setInitialSeqNo(getInitialSequenceNumber());
		respHandshakePkt.setConnectionType(-1);
		
		//TODO how is the socket ID exchanged.... 
		//respHandshakePkt.setSocketID(socketID);
		
		endPoint.doSend(respHandshakePkt);
	}




}

