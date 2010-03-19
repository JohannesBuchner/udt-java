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
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import udt.packets.Destination;
import udt.packets.PacketFactory;
import udt.util.UDTThreadFactory;

/**
 * the UDPEndpoint takes care of sending and receiving UDP network packets,
 * dispatching them to the correct {@link UDTSession}
 * 
 * 
 */
public class UDPEndPoint {

	private static final Logger logger=Logger.getLogger(ClientSession.class.getName());

	private final int port;

	private final DatagramSocket dgSocket;

	//remote destinations
	private final Map<Long,Destination>destinations;

	//sessions
	private final Map<Long,UDTSession>sessions;

	//last received packet
	private UDTPacket lastPacket;

	//if the endpoint is configured for a server socket,
	//this queue is used to handoff new UDTSessions to the application
	private final SynchronousQueue<UDTSession> sessionHandoff;
	private boolean serverSocketMode=false;

	//has the endpoint been stopped?
	private volatile boolean stopped=false;

	public static final int DATAGRAM_SIZE=61440;
	
	/**
	 * bind to any local port on the given host address
	 * @param localAddress
	 * @throws SocketException
	 * @throws UnknownHostException
	 */
	public UDPEndPoint(InetAddress localAddress)throws SocketException, UnknownHostException{
		this(localAddress,0);
	}
	
	/**
	 * Bind to the given address and port
	 * @param localAddress
	 * @param localPort - the port to bind to. If the port is zero, the system will pick an ephemeral port.
	 * @throws SocketException
	 * @throws UnknownHostException
	 */
	public UDPEndPoint(InetAddress localAddress, int localPort)throws SocketException, UnknownHostException{
		if(localAddress==null){
			dgSocket=new DatagramSocket(localPort, localAddress);
		}else{
			dgSocket=new DatagramSocket(localPort);
		}
		if(localPort>0)this.port = localPort;
		else port=dgSocket.getLocalPort();
		destinations=new ConcurrentHashMap<Long, Destination>();
		sessions=new ConcurrentHashMap<Long, UDTSession>();
		sessionHandoff=new SynchronousQueue<UDTSession>();
		//set a time out to avoid blocking in doReceive()
		dgSocket.setSoTimeout(1000);
	}

	/**
	 * bind to the default network interface on the machine
	 * 
	 * @param localPort - the port to bind to. If the port is zero, the system will pick an ephemeral port.
	 * @throws SocketException
	 * @throws UnknownHostException
	 */
	public UDPEndPoint(int localPort)throws SocketException, UnknownHostException{
		this(null,localPort);
	}

	/**
	 * bind to an ephemeral port on the default network interface on the machine
	 * 
	 * @throws SocketException
	 * @throws UnknownHostException
	 */
	public UDPEndPoint()throws SocketException, UnknownHostException{
		this(null,0);
	}

	/**
	 * start the endpoint. If the serverSocketModeEnabled flag is <code>true</code>,
	 * a new connection can be handed off to an application. The application needs to
	 * call #accept() to get the socket
	 * @param serverSocketModeEnabled
	 */
	public void start(boolean serverSocketModeEnabled){
		serverSocketMode=serverSocketModeEnabled;
		//start receive thread
		Runnable receive=new Runnable(){
			public void run(){
				try{
					while(!stopped){
						doReceive();
					}
				}catch(Exception ex){
					ex.printStackTrace();
				}
			}
		};
		Thread t=UDTThreadFactory.get().newThread(receive);
		t.setDaemon(true);
		t.start();
		logger.info("UDTEndpoint started.");
	}

	public void start(){
		start(false);
	}

	public void stop(){
		stopped=true;
		dgSocket.close();
	}

	/**
	 * @return the port which this client is bound to
	 */
	public int getLocalPort() {
		return this.dgSocket.getLocalPort();
	}
	/**
	 * @return Gets the local address to which the socket is bound
	 */
	public InetAddress getLocalAddress(){
		return this.dgSocket.getLocalAddress();
	}

	DatagramSocket getSocket(){
		return dgSocket;
	}

	UDTPacket getLastPacket(){
		return lastPacket;
	}

	public void addDestination(Long destinationID, Destination address){
		destinations.put(destinationID, address);
		logger.info(toString()+" destination <"+destinationID+"> = "+address);
	}

	public void addSession(Long destinationID,UDTSession session){
		sessions.put(destinationID, session);
	}

	public UDTSession getSession(Long destinationID){
		return sessions.get(destinationID);
	}

	/**
	 * wait the given time for a new connection
	 * @param timeout - the time to wait
	 * @param unit - the {@link TimeUnit}
	 * @return a new {@link UDTSession}
	 * @throws InterruptedException
	 */
	protected UDTSession accept(long timeout, TimeUnit unit)throws InterruptedException{
		return sessionHandoff.poll(timeout, unit);
	}

	/**
	 * single receive, run in the receiverThread, see {@link #start()}
	 * <ul>
	 * <li>Receives UDP packets from the network</li> 
	 * <li>Converts them to UDT packets</li>
	 * <li>dispatches the UDT packets according to their destination ID.</li>
	 * </ul> 
	 * @throws IOException
	 */
	protected void doReceive()throws IOException{
		try{
			//handshake sequence will ensure that the buffer size
			//need not be larger than this...
			DatagramPacket dp= new DatagramPacket(new byte[DATAGRAM_SIZE],DATAGRAM_SIZE);
			try{
				//will block until a packet is received or timeout has expired
				dgSocket.receive(dp);
				Destination peer=new Destination(dp.getAddress(), dp.getPort());
				int l=dp.getLength();
				UDTPacket p=PacketFactory.createPacket(dp.getData(),l);
				lastPacket=p;

				//dispatch
				UDTSession session=sessions.get(p.getDestinationID());
				if(session==null){
					session=new ServerSession(dp,this);
					addSession(p.getDestinationID(),session);
					if(serverSocketMode){
						sessionHandoff.put(session);
					}
				}
				session.received(p,peer);
				
			}catch(SocketTimeoutException ste){
				//can safely ignore... we will retry until the endpoint is stopped
			}
			
		}catch(Exception ex){
			logger.log(Level.WARNING, "Got: "+ex.getMessage());
		}
	}

	protected void doSend(UDTPacket packet)throws IOException{
		byte[]data=packet.getEncoded();
		Long dID=packet.getDestinationID();
		Destination dest=destinations.get(dID);
		if(dest==null)throw new IOException("Destination "+dID+" unknown.");
		DatagramPacket dgp = new DatagramPacket(data, data.length,
				dest.getAddress() , dest.getPort());
		dgSocket.send(dgp);
	}

	public String toString(){
		return  "UDPEndpoint port="+port;
	}

	public void sendRaw(DatagramPacket p)throws IOException{
		dgSocket.send(p);
	}
}
