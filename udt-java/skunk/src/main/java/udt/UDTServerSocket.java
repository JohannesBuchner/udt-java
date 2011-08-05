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
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.channels.ServerSocketChannel;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;



public class UDTServerSocket extends ServerSocket {
	private static final Logger logger=Logger.getLogger(UDTClient.class.getName());
	
    private volatile UDPEndPoint endpoint;
    private volatile InetAddress localAdd;
    private volatile int locPort;
    private volatile SocketAddress localSocketAddress;
	
    private volatile boolean started=false;
    private volatile boolean bound = false;
	private volatile boolean shutdown=false;
	
	/**
	 * create a UDT ServerSocket
     * @param localSocketAddress
	 * @param port - the local port. If 0, an ephemeral port will be chosen
	 */
    public UDTServerSocket(InetAddress localAddress, int port)throws UnknownHostException, IOException{
        super();
        endpoint= UDPEndPoint.get(localAddress,port);
        localAdd = localAddress;
        locPort = port;
        bound = true;
		logger.info("Created server endpoint on port "+endpoint.getLocalPort());
	}

	//starts a server on localhost
    public UDTServerSocket(int port)throws IOException,UnknownHostException{
		this(InetAddress.getLocalHost(),port);
	}
	
	/**
	 * listens and blocks until a new client connects and returns a valid {@link UDTSocket}
	 * for the new connection
	 * @return
	 */
@Override
    public synchronized Socket accept() throws IOException{
		if(!started){
			endpoint.start(true);
			started=true;
		}
        // TODO: use a blocking queue.
		while(!shutdown){
            try {
                UDTSession session = endpoint.accept(10000, TimeUnit.MILLISECONDS, null);
			if(session!=null){
				//wait for handshake to complete
				while(!session.isReady() || session.getSocket()==null){
					Thread.sleep(100);
				}
				return session.getSocket();
			}
            } catch (InterruptedException ex) {
                throw new IOException(ex);
		}
	} 
            throw new IOException("UDTSession was null");
    } 
	
    public UDPEndPoint getEndpoint(){
            return endpoint;
    }
        
    @Override
    public void bind(SocketAddress endpoint){
        //TODO: Implement ServerSocket.bind
    }
    
    @Override
    public void bind(SocketAddress endpoint, int timeout){
        //TODO: Implement ServerSocket.bind
    }
    
    @Override
    public void close(){
		shutdown=true;
        // TODO: The endpoint might have other ServerSocket's listening, 
        // we need to pass the endpoint the socket, or session or something
        // the endpoint should only stop when it has no remaining sessions.
		endpoint.stop();
	}
	
    @Override
    public ServerSocketChannel getChannel(){
        return null;
	}
    
    @Override
    public InetAddress getInetAddress(){
        return localAdd;
}
    
    @Override
    public int getLocalPort(){
        return locPort;
    }
    
    @Override
    public SocketAddress getLocalSocketAddress(){
        return localSocketAddress;
    }
    
    @Override
    public int getReceiveBufferSize(){
        return 0;
    }
    
    @Override
    public boolean getReuseAddress(){
        return false;
    }
    
    @Override
    public int getSoTimeout(){
        return 0;
    }
    
    @Override
    public boolean isBound(){
        return started;
    }
    
    @Override
    public boolean isClosed(){
        return shutdown;
    }
    
    @Override
    public void setPerformancePreferences(int connectionTime, int latency, int bandwidth){
        
    }
    
    @Override
    public void setReceiveBufferSize(int size){
        
    }
    
    @Override
    public void setReuseAddress(boolean on){
        
    }
    
    @Override
    public void setSoTimeout(int timeout){
        
    }
    
    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder(120);
        sb.append("UDTServerSocket: \n");
        //TODO: add statistics.
        return sb.toString();
    }
    
}
