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
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import udt.packets.ConnectionHandshake;
import udt.packets.UDTSocketAddress;
import udt.packets.PacketFactory;
import udt.util.ObjectPool;
import udt.util.UDTThreadFactory;

/**
 * the UDPMultiplexer takes care of sending and receiving UDP network packets,
 * dispatching them to the correct {@link UDTSession}
 */
public class UDPMultiplexer {

    //class fields
	private static final Logger logger=Logger.getLogger(ClientSession.class.getName());
    public static final int DATAGRAM_SIZE=1400;

    
    //class methods
    private static final WeakHashMap<SocketAddress, UDPMultiplexer> localEndpoints
            = new WeakHashMap<SocketAddress, UDPMultiplexer>();
    
    public static UDPMultiplexer get(DatagramSocket socket){
        SocketAddress localInetSocketAddress = null;
        UDPMultiplexer result = null;
        if ( socket.isBound()){
            SocketAddress sa = socket.getLocalSocketAddress();
            if ( sa instanceof InetSocketAddress ){
                localInetSocketAddress = (InetSocketAddress) sa;
            } else {
                // Must be a special DatagramSocket impl or extended.
                localInetSocketAddress = 
                    new InetSocketAddress(socket.getLocalAddress(), socket.getLocalPort());
            }
            synchronized (localEndpoints){
                result = localEndpoints.get(localInetSocketAddress);
            }
        }
        if (result != null) return result;
        try {
            result = new UDPMultiplexer(socket);
            if (localInetSocketAddress == null){
                // The DatagramSocket was unbound, it should be bound now.
                localInetSocketAddress = 
                    new InetSocketAddress(socket.getLocalAddress(), socket.getLocalPort());
            }
        } catch (SocketException ex) {
            Logger.getLogger(UDPMultiplexer.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (result != null){
            synchronized (localEndpoints){
                UDPMultiplexer exists = localEndpoints.get(localInetSocketAddress);
                if (exists != null && exists.getSocket().equals(socket)) result = exists;
                // Only cache if a record doesn't already exist.
                else if (exists == null) localEndpoints.put(localInetSocketAddress, result);
            }
        }
        return result; // may be null.           
    }
    
    
    
    public static UDPMultiplexer get(InetAddress localAddress, int localPort){
        InetSocketAddress localInetSocketAddress = new InetSocketAddress(localAddress, localPort);
        return get(localInetSocketAddress);
    }
    
    public static UDPMultiplexer get(SocketAddress localSocketAddress){
        InetSocketAddress localInetSocketAddress = null;
        if (localSocketAddress instanceof InetSocketAddress){
            localInetSocketAddress = (InetSocketAddress) localSocketAddress;
        } else if (localSocketAddress instanceof UDTSocketAddress){
            UDTSocketAddress udtSA = (UDTSocketAddress) localSocketAddress;
            localInetSocketAddress = 
                    new InetSocketAddress(udtSA.getAddress(), udtSA.getPort());
        }
        if (localInetSocketAddress == null) return null;
        UDPMultiplexer result = null;
        synchronized (localEndpoints){
            result = localEndpoints.get(localInetSocketAddress);
        }
        if (result != null) return result;
        try {
            result = new UDPMultiplexer(localInetSocketAddress);
            if (localInetSocketAddress.getPort() == 0 || 
                    localInetSocketAddress.getAddress().isAnyLocalAddress()){
                // ephemeral port or wildcard address, bind operation is complete.
                localInetSocketAddress = 
                        new InetSocketAddress(result.getLocalAddress(), result.getLocalPort());
            }
        } catch (SocketException ex) {
            logger.log(Level.SEVERE, null, ex);
        } catch (UnknownHostException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
        if (result != null){
            synchronized (localEndpoints){
                UDPMultiplexer exists = localEndpoints.get(localInetSocketAddress);
                if (exists != null) result = exists;
                else localEndpoints.put(localInetSocketAddress, result);
            }
        }
        return result; // may be null.      
    }
    
    /**
     * Allows a custom endpoint to be added to the pool.
     * @param endpoint 
     */
    public static void put(UDPMultiplexer endpoint){
        SocketAddress local = endpoint.getSocket().getLocalSocketAddress();
        synchronized (localEndpoints){
            localEndpoints.put(local, endpoint);
        }
    }
    
    //object fields
	private final int port;

	private final DatagramSocket dgSocket;

	//active sessions keyed by socket ID
    private final Map<Integer,UDTSession>sessions=new ConcurrentHashMap<Integer, UDTSession>();

	//last received packet
	private UDTPacket lastPacket;

	//if the endpoint is configured for a server socket,
	//this queue is used to handoff new UDTSessions to the application
	private final SynchronousQueue<UDTSession> sessionHandoff=new SynchronousQueue<UDTSession>();
	
    private final ObjectPool<BlockingQueue<UDTSession>> queuePool 
            = new ObjectPool<BlockingQueue<UDTSession>>(20);
    
    private final ConcurrentMap<Integer, BlockingQueue<UDTSession>> handoff
            = new ConcurrentHashMap<Integer, BlockingQueue<UDTSession>>(120);
    
    // registered sockets
    private final Set<Integer> registeredSockets = new HashSet<Integer>(120);
    // registered sockets lock
    private final ReadWriteLock rwl = new ReentrantReadWriteLock();
    private final Lock readSocketLock = rwl.readLock();
    private final Lock writeSocketLock = rwl.writeLock();
    
    
	private boolean serverSocketMode=false;

	//has the endpoint been stopped?
	private volatile boolean stopped=false;

    private final AtomicInteger nextSocketID=new AtomicInteger(20+new Random().nextInt(5000));


	/**
	 * create an endpoint on the given socket
	 * 
	 * @param socket -  a UDP datagram socket
     * @throws SocketException  
	 */
    protected UDPMultiplexer(DatagramSocket socket) throws SocketException{
		this.dgSocket=socket;
            if (!socket.isBound()){
                socket.bind(null);
            }
		port=dgSocket.getLocalPort();
	}
	
	/**
	 * bind to any local port on the given host address
	 * @param localAddress
	 * @throws SocketException
	 * @throws UnknownHostException
	 */
    private UDPMultiplexer(InetAddress localAddress)throws SocketException, UnknownHostException{
		this(localAddress,0);
	}

	/**
	 * Bind to the given address and port
	 * @param localAddress
	 * @param localPort - the port to bind to. If the port is zero, the system will pick an ephemeral port.
	 * @throws SocketException
	 * @throws UnknownHostException
	 */
    private UDPMultiplexer(InetAddress localAddress, int localPort)throws SocketException, UnknownHostException{
		if(localAddress==null){
			dgSocket=new DatagramSocket(localPort, localAddress);
		}else{
			dgSocket=new DatagramSocket(localPort);
		}
		if(localPort>0)this.port = localPort;
		else port=dgSocket.getLocalPort();
		
		configureSocket();
	}

    private UDPMultiplexer (InetSocketAddress localSocketAddress) 
            throws SocketException, UnknownHostException {
        dgSocket = new DatagramSocket(localSocketAddress);
        port = dgSocket.getLocalPort();
        configureSocket();
    }

	protected void configureSocket()throws SocketException{
		//set a time out to avoid blocking in doReceive()
		dgSocket.setSoTimeout(100000);
		//buffer size
		dgSocket.setReceiveBufferSize(128*1024);
		dgSocket.setReuseAddress(false);
	}
	
	/**
	 * bind to the default network interface on the machine
	 * 
	 * @param localPort - the port to bind to. If the port is zero, the system will pick an ephemeral port.
	 * @throws SocketException
	 * @throws UnknownHostException
	 */
	public UDPMultiplexer(int localPort)throws SocketException, UnknownHostException{
		this(null,localPort);
	}

	/**
	 * bind to an ephemeral port on the default network interface on the machine
	 * 
	 * @throws SocketException
	 * @throws UnknownHostException
	 */
	public UDPMultiplexer()throws SocketException, UnknownHostException{
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
					doReceive();
				}catch(Exception ex){
					logger.log(Level.WARNING,"",ex);
				}
			}
		};
		Thread t=UDTThreadFactory.get().newThread(receive);
		t.setName("UDPEndpoint-"+t.getName());
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
     * Provides assistance to a socket to determine a random socket id,
     * every caller receives a unique value.  This value is unique at the
     * time of calling, however it may not be at registration time.
     * 
     * This socketID has not been registered, all socket ID's must be 
     * registered or connection will fail.
     * @return
     */
    public int getUniqueSocketID(){
        Integer socketID = nextSocketID.getAndIncrement();
        try{
            readSocketLock.lock();
            while (registeredSockets.contains(socketID)){
                socketID = nextSocketID.getAndIncrement();
            }
            return socketID; // should we register it?
        } finally {
            readSocketLock.unlock();
        }
    }
    
    void registerSocketID(int socketID, UDTSocket socket) throws SocketException {
        if (!equals(socket.getEndpoint())) throw new SocketException (
                "Socket doesn't originate for this endpoint: "
                + socket.toString());
        try {
            writeSocketLock.lock();
            if (registeredSockets.contains(socketID)){
                throw new SocketException("Already registered, Socket ID: " +socketID);
            }
            registeredSockets.add(socketID);
        }finally{
            writeSocketLock.unlock();
        }
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

    public void addSession(Integer destinationID,UDTSession session){
            logger.log(Level.INFO, "Storing session <{0}>", destinationID);
		sessions.put(destinationID, session);
	}

	public UDTSession getSession(Long destinationID){
		return sessions.get(destinationID);
	}

	public Collection<UDTSession> getSessions(){
		return sessions.values();
	}

	/**
	 * wait the given time for a new connection
	 * @param timeout - the time to wait
	 * @param unit - the {@link TimeUnit}
     * @param socketID - the socket id.
	 * @return a new {@link UDTSession}
	 * @throws InterruptedException
	 */
    protected UDTSession accept(long timeout, TimeUnit unit, Integer socketID)throws InterruptedException{
            //return sessionHandoff.poll(timeout, unit);
            BlockingQueue<UDTSession> session = handoff.get(socketID);
            try {
                if (session == null){
                    session = queuePool.get();
                    if (session == null) {
                        session = new ArrayBlockingQueue<UDTSession>(1);
	}
                    BlockingQueue<UDTSession> existed = handoff.putIfAbsent(socketID,session);
                    if (existed != null){
                        session = existed;
                    }
                }
                return session.poll(timeout, unit);
            } finally {
                boolean removed = handoff.remove(socketID, session);
                if (removed){
                    session.clear();
                    queuePool.accept(session);
                }
            }
    }


	final DatagramPacket dp= new DatagramPacket(new byte[DATAGRAM_SIZE],DATAGRAM_SIZE);

	/**
	 * single receive, run in the receiverThread, see {@link #start()}
	 * <ul>
	 * <li>Receives UDP packets from the network</li> 
	 * <li>Converts them to UDT packets</li>
	 * <li>dispatches the UDT packets according to their destination ID.</li>
	 * </ul> 
	 * @throws IOException
	 */
	private long lastDestID=-1;
	private UDTSession lastSession;
	
	private int n=0;
	
	private final Object lock=new Object();
	
	protected void doReceive()throws IOException{
		while(!stopped){
			try{
					//will block until a packet is received or timeout has expired
					dgSocket.receive(dp);
                UDTSocketAddress peer= null;
					int l=dp.getLength();
					UDTPacket packet=PacketFactory.createPacket(dp.getData(),l);
					lastPacket=packet;
					//handle connection handshake 
					if(packet.isConnectionHandshake()){
						synchronized(lock){
							Long id=Long.valueOf(packet.getDestinationID());
							UDTSession session=sessions.get(id);
                        if(session==null){ // What about DOS?
								session=new ServerSession(dp,this);
								addSession(session.getSocketID(),session);
								//TODO need to check peer to avoid duplicate server session
								if(serverSocketMode){
									logger.fine("Pooling new request.");
//                                sessionHandoff.put(session); // blocking method, what about offer?
                                BlockingQueue<UDTSession> queue = handoff.get(session.getSocketID());
                                if (queue != null){
                                    boolean success = queue.offer(session);
                                    if (success){
									logger.fine("Request taken for processing.");
                                    } else {
                                        logger.fine("Request discarded, queue full.");
								}
                                } else {
                                    logger.fine("No ServerSocket listening at socketID: "
                                            + session.getSocketID() 
                                            + "to answer request");
							}
                            }
                        }
							peer.setSocketID(((ConnectionHandshake)packet).getSocketID());
                        peer = new UDTSocketAddress(dp.getAddress(), dp.getPort(), 
                                ((ConnectionHandshake)packet).getSocketID());
							session.received(packet,peer);
						}
					}
					else{
						//dispatch to existing session
						long dest=packet.getDestinationID();
						UDTSession session;
						if(dest==lastDestID){
							session=lastSession;
						}
						else{
							session=sessions.get(dest);
							lastSession=session;
							lastDestID=dest;
						}
						if(session==null){
							n++;
							if(n%100==1){
                            logger.warning("Unknown session <"+dest
                                    +"> requested from <"+peer+"> packet type "
                                    +packet.getClass().getName());
							}
						}
						else{
							session.received(packet,peer);
						}
					}
				}catch(SocketException ex){
					logger.log(Level.INFO, "SocketException: "+ex.getMessage());
				}catch(SocketTimeoutException ste){
					//can safely ignore... we will retry until the endpoint is stopped
			}catch(Exception ex){
				logger.log(Level.WARNING, "Got: "+ex.getMessage(),ex);
			}
		}
	}

	protected void doSend(UDTPacket packet)throws IOException{
		byte[]data=packet.getEncoded();
		DatagramPacket dgp = packet.getSession().getDatagram();
		dgp.setData(data);
		dgSocket.send(dgp);
	}

	public String toString(){
		return  "UDPEndpoint port="+port;
	}

	public void sendRaw(DatagramPacket p)throws IOException{
		dgSocket.send(p);
	}
}
