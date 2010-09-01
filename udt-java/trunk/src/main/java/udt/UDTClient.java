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


	public UDTClient(InetAddress address, int localport)throws SocketException, UnknownHostException{
		//create endpoint
		clientEndpoint=new UDPEndPoint(address,localport);
		logger.info("Created client endpoint on port "+localport);
	}

	public UDTClient(InetAddress address)throws SocketException, UnknownHostException{
		//create endpoint
		clientEndpoint=new UDPEndPoint(address);
		logger.info("Created client endpoint on port "+clientEndpoint.getLocalPort());
	}

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
		InetAddress address=InetAddress.getByName(host);
		Destination destination=new Destination(address,port);
		//create client session...
		clientSession=new ClientSession(clientEndpoint,destination);
		clientEndpoint.addSession(clientSession.getSocketID(), clientSession);

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
			shutdown.setDestinationID(clientSession.getDestination().getSocketID());
			shutdown.setSession(clientSession);
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
