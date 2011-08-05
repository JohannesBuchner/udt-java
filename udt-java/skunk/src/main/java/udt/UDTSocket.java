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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketOptions;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import udt.packets.UDTSocketAddress;
/**
 * UDTSocket is analogous to a normal java.net.Socket, it provides input and 
 * output streams for the application
 * 
 * TODO is it possible to actually extend java.net.Socket ?
 * 
 */
public class UDTSocket extends Socket{

    private static final ArrayList<UDTSocketAddress> boundSockets 
            = new ArrayList<UDTSocketAddress>(120);

	//endpoint
	private volatile UDPMultiplexer endpoint;

	private volatile boolean active;
        private volatile boolean connected;
        private volatile boolean bound;
        private volatile boolean shutIn; // receiver closed.
        private volatile boolean shutOut; // sender closed.
        private volatile boolean closed;

	//processing received data
	private volatile UDTReceiver receiver;
	private volatile UDTSender sender;

	private volatile UDTSession session;

	private volatile UDTInputStream inputStream;
	private volatile UDTOutputStream outputStream;
        
        private volatile UDTSocketAddress localSocketAddress;
        private volatile UDTSocketAddress destination;
	/**
         * The session is usually the caller for this constructor
         * so the session already knows this is the socket.
	 * @param host
	 * @param port
	 * @param endpoint
	 * @throws SocketException,UnknownHostException
	 */
	UDTSocket(UDPMultiplexer endpoint, UDTSession session)throws SocketException,UnknownHostException{
            super();
		this.endpoint=endpoint;
		this.session=session;
		this.receiver=new UDTReceiver(session,endpoint);
		this.sender=new UDTSender(session,endpoint);
            localSocketAddress = new UDTSocketAddress(endpoint.getLocalAddress(), 
                endpoint.getLocalPort(), session.getSocketID());
            destination = session.getDestination();
            bound = true;
	}

        public UDTSocket(InetAddress host, int port ) throws SocketException,
                UnknownHostException{
            super();
            this.endpoint = UDPMultiplexer.get(host, port);
            this.session = null;
            this.receiver = null;
            this.sender = null;
            active = false;
            bound = true;
        
        }
        
        public UDTSocket(){
            super();
            endpoint = null;
            session = null;
            receiver = null;
            sender = null;
            active = false;
            bound = false;
        }
        
    @Override
        public void connect(SocketAddress destination) throws IOException {
	connect(destination, 0);
    }

    @Override
    public void connect(SocketAddress destination, int timeout) throws IOException {
	if (destination == null) throw new IllegalArgumentException("connect: The address can't be null");
	if (timeout < 0) throw new IllegalArgumentException("connect: timeout can't be negative");
	if (isClosed()) throw new SocketException("Socket is closed");
        if (isConnected()) throw new SocketException("already connected");
	if (!(destination instanceof UDTSocketAddress)) 
            throw new IllegalArgumentException("Unsupported address type");

	UDTSocketAddress epoint = (UDTSocketAddress) destination;
        InetAddress addr = epoint.getAddress();
        int port = epoint.getPort();

	SecurityManager security = System.getSecurityManager();
	if (security != null) {
            security.checkConnect(addr.getHostAddress(),port);
	}
        if (session == null){
            session = new ClientSession(endpoint, (UDTSocketAddress) destination);
            session.setSocket(this);
        }
        endpoint.addSession(session.getSocketID(), session);
        receiver = new UDTReceiver(session, endpoint);
        sender = new UDTSender(session, endpoint);
        localSocketAddress = new UDTSocketAddress(endpoint.getLocalAddress(), 
                endpoint.getLocalPort(), session.getSocketID());
        endpoint.start();
        try {
            ((ClientSession)session).connect();
        } catch (InterruptedException ex) {
            throw new IOException(ex);
        }
        destination = session.getDestination();
        connected = true;
	/*
	 * If the socket was not bound before the connect, it is now because
	 * the kernel will have picked an ephemeral port & a local address
	 */
	bound = true;
    }

    /**
     * Binds the socket to a local address.
     * <P>
     * If the address is <code>null</code>, then the system will pick up
     * an ephemeral port and a valid local address to bind the socket.
     *
     * @param	bindpoint the <code>SocketAddress</code> to bind to
     * @throws	IOException if the bind operation fails, or if the socket
     *			   is already bound.
     * @throws  IllegalArgumentException if bindpoint is a
     *          SocketAddress subclass not supported by this socket
     *
     * @since	1.4
     * @see #isBound
     */
    public void bind(SocketAddress bindpoint) throws IOException {
        if (!(bindpoint instanceof UDTSocketAddress))
            throw new IllegalArgumentException("Unsupported SocketAddress type");
        if (isBound()) throw new IOException("Socket already bound");
        synchronized (boundSockets) {
            if (boundSockets.contains(bindpoint)) throw 
                    new IOException("A socket is already bound to this address");
        }
        endpoint = UDPMultiplexer.get(bindpoint);
        if (endpoint == null) throw new SocketException("Failed to bind to UDPEndPoint");
	bound = true;
    }
    
    @Override
    public InetAddress getInetAddress() {
	if (!isConnected()) return null;
        return destination.getAddress();
    }
        
    // Inherit javadoc.
    @Override
    public InetAddress getLocalAddress() {
	// This is for backward compatibility only, the super is not bound and
        // returns InetAddress.anyLocalAddress();
	if (!isBound()) super.getLocalAddress();
        return localSocketAddress.getAddress();
    }

    // Inherit javadoc.
    @Override
    public int getPort() {
	if (!isConnected()) return 0;
	return destination.getPort();
    }

    // Inherit javadoc.
    @Override
    public int getLocalPort() {
	if (!isBound()) return -1;
        return localSocketAddress.getPort();
    }

    // Inherit javadoc.
    @Override
    public SocketAddress getRemoteSocketAddress() {
	if (!isConnected()) return null;
	return destination;
    }

    // Inherit javadoc.
    @Override
    public SocketAddress getLocalSocketAddress() {
	if (!isBound()) return null;
	return localSocketAddress;
    }

    @Override
    public SocketChannel getChannel() {
	return null;
    }

    /**
     * Not supported
     * @param on
     * @throws SocketException 
     */
    @Override
    public void setTcpNoDelay(boolean on) throws SocketException {
	if (isClosed()) throw new SocketException("Socket closed");
	setOption(SocketOptions.TCP_NODELAY, Boolean.valueOf(on));
    }

    /**
     * Not supported
     * @return false
     * @throws SocketException 
     */
    @Override
    public boolean getTcpNoDelay() throws SocketException {
	if (isClosed()) throw new SocketException("Socket closed");
	return ((Boolean) getOption(SocketOptions.TCP_NODELAY)).booleanValue();
    }

    /**
     * Not supported.
     * @param on
     * @param linger
     * @throws SocketException 
     */
    @Override
    public void setSoLinger(boolean on, int linger) throws SocketException {
	if (isClosed()) throw new SocketException("Socket closed");
        if (linger < 0) throw new IllegalArgumentException("SO_LINGER cannot be less than zero");
        // do nothing, not supported.
    }

    @Override
    public int getSoLinger() throws SocketException {
	if (isClosed()) throw new SocketException("Socket closed");
	return -1; // implies this option is disabled
    }

    // Sending of urgent data, should we support it?
    @Override
    public void sendUrgentData (int data) throws IOException  {
        throw new SocketException ("Urgent data not supported");
    }

    // Sending of urgent data, should we enable it?
    @Override
    public void setOOBInline(boolean on) throws SocketException {
	if (isClosed()) throw new SocketException("Socket closed");
	setOption(SocketOptions.SO_OOBINLINE, Boolean.valueOf(on));
    }

    @Override
    public boolean getOOBInline() throws SocketException {
	if (isClosed()) throw new SocketException("Socket closed");
	return ((Boolean) getOption(SocketOptions.SO_OOBINLINE)).booleanValue();
    }

    // TODO: implement set socket timeout.
    @Override
    public void setSoTimeout(int timeout) throws SocketException {
	if (isClosed()) throw new SocketException("Socket closed");
	if (timeout < 0) throw new IllegalArgumentException("negative timeout not allowed");
        setOption(SocketOptions.SO_TIMEOUT, new Integer(timeout));
    }

    // TODO: Implement get socket timeout.
    @Override
    public synchronized int getSoTimeout() throws SocketException {
	if (isClosed()) throw new SocketException("Socket closed");
	Object o = getOption(SocketOptions.SO_TIMEOUT);
	if (o instanceof Integer) return ((Integer) o).intValue();
	return 0;	
    }

     // object method signature compatibility only, not currently supported.  
    @Override
    public void setSendBufferSize(int size)
    throws SocketException{
	if (!(size > 0)) throw new IllegalArgumentException("negative send size not allowed");
	if (isClosed()) throw new SocketException("Socket closed");
	setOption(SocketOptions.SO_SNDBUF, new Integer(size));
    }

   // object method signature compatibility only, not currently supported.
    @Override
    public int getSendBufferSize() throws SocketException {
	if (isClosed()) throw new SocketException("Socket is closed");
	int result = 0;
	Object o = getOption(SocketOptions.SO_SNDBUF);
	if (o instanceof Integer) result = ((Integer)o).intValue();
	return result;
    }

    // object method signature compatibility only, not currently supported.
    @Override
    public void setReceiveBufferSize(int size)
    throws SocketException{
	if (size <= 0) throw new IllegalArgumentException("invalid receive size");
	if (isClosed()) throw new SocketException("Socket closed");
        setOption(SocketOptions.SO_RCVBUF, new Integer(size));
    }
    
    // object method signature compatibility only, not currently supported.
    @Override
    public int getReceiveBufferSize()
    throws SocketException{
	if (isClosed()) throw new SocketException("Socket closed");
	int result = 0;
	Object o = getOption(SocketOptions.SO_RCVBUF);
	if (o instanceof Integer) {
	    result = ((Integer)o).intValue();
	}
	return result;
    }

    // TODO: Implement keep alive.
    @Override
    public void setKeepAlive(boolean on) throws SocketException {
	if (isClosed())
	    throw new SocketException("Socket is closed");
        setOption(SocketOptions.SO_KEEPALIVE, Boolean.valueOf(on));
    }

 
    @Override
    public boolean getKeepAlive() throws SocketException {
        // Keep alive is not currently implemented in the udt session.
	if (isClosed())
	    throw new SocketException("Socket is closed");
	return ((Boolean) getOption(SocketOptions.SO_KEEPALIVE)).booleanValue();
    }

    public void setTrafficClass(int tc) throws SocketException {
        // Safe to ignore, not supported.
    }

    public int getTrafficClass() throws SocketException {
        // Call redirected to underlying DatagramSocket.
        return endpoint.getSocket().getTrafficClass();
    }

    @Override
    public void setReuseAddress(boolean on) throws SocketException {
        throw new SocketException("SO_REUSEADDR not supported");
    }

    /**
     * Tests if SO_REUSEADDR is enabled.
     *
     * @return a <code>boolean</code> indicating whether or not SO_REUSEADDR is enabled.
     * @exception SocketException if there is an error
     * in the underlying protocol, such as a TCP error. 
     * @since   1.4
     * @see #setReuseAddress(boolean)
     */
    @Override
    public boolean getReuseAddress() throws SocketException {
	if (isClosed()) throw new SocketException("Socket is closed");
	return ((Boolean) (getOption(SocketOptions.SO_REUSEADDR))).booleanValue();
    }

    /**
     * Places the input stream for this socket at "end of stream".
     * Any data sent to the input stream side of the socket is acknowledged
     * and then silently discarded.
     * <p>
     * If you read from a socket input stream after invoking 
     * shutdownInput() on the socket, the stream will return EOF.
     *
     * @exception IOException if an I/O error occurs when shutting down this
     * socket.
     *
     * @since 1.3
     * @see java.net.Socket#shutdownOutput()
     * @see java.net.Socket#close()
     * @see java.net.Socket#setSoLinger(boolean, int)
     * @see #isInputShutdown
     */
    @Override
    public void shutdownInput() throws IOException
    {
	if (isClosed()) throw new SocketException("Socket closed");
	if (!isConnected()) throw new SocketException("Socket not connected");
	if (isInputShutdown()) throw new SocketException("Socket input already shutdown");
	receiver.stop();
        inputStream.close();
	shutIn = true;
    }
    
    @Override
    public void shutdownOutput() throws IOException
    {
	if (isClosed()) throw new SocketException("Socket closed");
	if (!isConnected()) throw new SocketException("Socket is not connected");
	if (isOutputShutdown()) throw new SocketException("Socket output is already shutdown");
        sender.stop();
        outputStream.close();
	shutOut = true;
    }

    /**
     * Returns the connection state of the socket.
     *
     * @return true if the socket successfuly connected to a server
     * @since 1.4
     */
    @Override
    public boolean isConnected() {
	// Before 1.3 Sockets were always connected during creation
	return connected;
    }

    @Override
    public boolean isBound() {
	return bound ;
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    @Override
    public boolean isInputShutdown() {
	return shutIn;
    }

    /**
     * Returns whether the write-half of the socket connection is closed.
     *
     * @return true if the output of the socket has been shutdown
     * @since 1.4
     * @see #shutdownOutput
     */
    @Override
    public boolean isOutputShutdown() {
	return shutOut;
    }
    
    // some preliminary support for socket options.
    private Object getOption(int optID) {
        return Boolean.FALSE;
    }
        
	public UDTReceiver getReceiver() {
		return receiver;
	}

	public void setReceiver(UDTReceiver receiver) {
		this.receiver = receiver;
	}

	public UDTSender getSender() {
		return sender;
	}

	public void setSender(UDTSender sender) {
		this.sender = sender;
	}

//	public void setActive(boolean active) {
//		this.active = active;
//	}

	public boolean isActive() {
		return active;
	}

	public UDPMultiplexer getEndpoint() {
		return endpoint;
	}

	/**
	 * get the input stream for reading from this socket
	 * @return
	 */
    @Override
	public synchronized InputStream getInputStream()throws IOException{
		if(inputStream==null){
			inputStream=new UDTInputStream(this);
		}
		return inputStream;
	}

	/**
	 * get the output stream for writing to this socket
	 * @return
	 */
    @Override
	public synchronized OutputStream getOutputStream(){
		if(outputStream==null){
			outputStream=new UDTOutputStream(this);
		}
		return outputStream;
	}

	public final UDTSession getSession(){
		return session;
	}

	/**
	 * write single block of data without waiting for any acknowledgment
	 * @param data
	 */
	protected void doWrite(byte[]data)throws IOException{
		doWrite(data, 0, data.length);

	}

	/**
	 * write the given data 
	 * @param data - the data array
	 * @param offset - the offset into the array
	 * @param length - the number of bytes to write
	 * @throws IOException
	 */
	protected void doWrite(byte[]data, int offset, int length)throws IOException{
		try{
			doWrite(data, offset, length, 10, TimeUnit.MILLISECONDS);
		}catch(InterruptedException ie){
			IOException io=new IOException();
			io.initCause(ie);
			throw io;
		}
	}

	/**
	 * write the given data, waiting at most for the specified time if the queue is full
	 * @param data
	 * @param offset
	 * @param length
	 * @param timeout
	 * @param units
	 * @throws IOException - if data cannot be sent
	 * @throws InterruptedException
	 */
	protected void doWrite(byte[]data, int offset, int length, int timeout, TimeUnit units)throws IOException,InterruptedException{
		ByteBuffer bb=ByteBuffer.wrap(data,offset,length);
		while(bb.remaining()>0){
			try{
				sender.sendUdtPacket(bb, timeout, units);
			}catch(Exception ex){
				ex.printStackTrace();
			}
		}
		if(length>0)active=true;
	}

	/**
	 * will block until the outstanding packets have really been sent out
	 * and acknowledged
         * 
         * @throws InterruptedException 
	 */
	protected void flush() throws InterruptedException{
		if(!active)return;
		final long seqNo=sender.getCurrentSequenceNumber();
		if(seqNo<0)throw new IllegalStateException();
		while(!sender.isSentOut(seqNo)){
			Thread.sleep(5);
		}
		if(seqNo>-1){
			//wait until data has been sent out and acknowledged
			while(active && !sender.haveAcknowledgementFor(seqNo)){
				sender.waitForAck(seqNo);
			}
		}
		//TODO need to check if we can pause the sender...
		//sender.pause();
	}

	//writes and wait for ack
	protected void doWriteBlocking(byte[]data)throws IOException, InterruptedException{
		doWrite(data);
		flush();
	}

    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder(400);
        sb  .append("UDTSocket: \n")
            .append("Local address: ")
            .append(localSocketAddress.toString())
            .append(" Destination address: ")
            .append(destination.toString())
            .append("\n")
            .append(session.getStatistics().toString());
        return sb.toString();
    }

	/**
	 * close the connection
	 * @throws IOException
	 */
    @Override
	public void close()throws IOException{
		if(inputStream!=null)inputStream.close();
		if(outputStream!=null)outputStream.close();
		active=false;
                closed = true;
                connected = false;
	}

    private void setOption(int SO_KEEPALIVE, Boolean valueOf) {
        // does nothing at this stage.
}

    private void setOption(int SO_RCVBUF, Integer integer) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

}
