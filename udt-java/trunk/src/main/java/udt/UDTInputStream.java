package udt;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import udt.util.FlowWindow;
import udt.util.UDTStatistics;

/**
 * The UDTInputStream receives data blocks from the {@link UDTSocket}
 * as they become available, and places them into an ordered, 
 * bounded queue (the flow window) for reading by the application
 * 
 * 
 */
public class UDTInputStream extends InputStream {

	//the socket owning this inputstream
	private final UDTSocket socket;

	//inbound application data, in-order, and ready for reading
	//by the application
	private final FlowWindow<AppData>appData;

	private final UDTStatistics statistics;

	//the highest sequence number read by the application
	private long highestSequenceNumber=-1;

	//set to 'false' by the receiver when it gets a shutdown signal from the peer
	//see the noMoreData() method
	private final AtomicBoolean expectMoreData=new AtomicBoolean(true);


	private final ByteBuffer buffer;
	
	private volatile boolean closed=false;
	
	private volatile boolean blocking=true;
	
	/**
	 * create a new {@link UDTInputStream} connected to the given socket
	 * @param socket - the {@link UDTSocket}
	 * @param statistics - the {@link UDTStatistics}
	 * @throws IOException
	 */
	public UDTInputStream(UDTSocket socket, UDTStatistics statistics)throws IOException{
		this.socket=socket;
		this.statistics=statistics;
		appData=new FlowWindow<AppData>(getFlowWindowSize());
		buffer=ByteBuffer.allocate(65536);
		buffer.flip();
	}

	private int getFlowWindowSize(){
		if(socket!=null)return socket.getSession().getFlowWindowSize();
		else return 64;
	}
	/**
	 * create a new {@link UDTInputStream} connected to the given socket
	 * @param socket - the {@link UDTSocket}
	 * @throws IOException
	 */
	public UDTInputStream(UDTSocket socket)throws IOException{
		this(socket, socket.getSession().getStatistics());
	}

	private final byte[]single=new byte[1];

	@Override
	public int read()throws IOException{
		int b=0;
		while(b==0)
			b=read(single);
		
		if(b>0){
			return single[0];
		}
		else {
			return b;
		}
	}
	
	private AppData currentChunk=null;
	int offset=0;
	@Override
	public int read(byte[]target)throws IOException{
		try{
			//empty the buffer first
			int read=readFromBuffer(target, 0);
			//if no more space left in target, exit now
			if(read==target.length){
				return target.length;
			}
			//otherwise try to fill up the buffer
			fillBuffer();
			read+=readFromBuffer(target, read);
			if(read>0)return read;
			if(closed)return -1;
			if(expectMoreData.get() || buffer.remaining()>0 || !appData.isEmpty())return 0;
			//no more data
			return -1;

		}catch(Exception ex){
			IOException e= new IOException();
			e.initCause(ex);
			throw e;
		}
	}

	@Override
	public int available()throws IOException{
		return buffer.remaining();
	}
	
	/**
	 * write as much data into the ByteBuffer as possible<br/>
	 * In blocking mode,this method will block until data is available or the socket is closed, 
	 * otherwise wait for at most 10 milliseconds.
	 * @returns <code>true</code> if data available
	 * @throws InterruptedException
	 */
	private boolean fillBuffer()throws IOException{
		if(currentChunk==null){
			try{
				if(blocking){
					currentChunk=appData.poll(1, TimeUnit.MILLISECONDS);
					while (!closed && currentChunk==null){
						currentChunk=appData.poll(1000, TimeUnit.MILLISECONDS);
					}
				}
				else currentChunk=appData.poll(10, TimeUnit.MILLISECONDS);
			}catch(InterruptedException ie){
				IOException ex=new IOException();
				ex.initCause(ie);
				throw ex;
			}
		}
		if(currentChunk!=null){
			//check if the data is in-order
			if(currentChunk.sequenceNumber==highestSequenceNumber+1){
				highestSequenceNumber++;
				statistics.updateReadDataMD5(currentChunk.data);
			}
			else if(currentChunk.sequenceNumber<=highestSequenceNumber){
				//duplicate, drop it
				currentChunk=null;
				statistics.incNumberOfDuplicateDataPackets();
				return false;
			}
			else{
				//out of order data, put back into queue
				appData.offer(currentChunk);
				currentChunk=null;
				return false;
			}
			
			//fill data into the buffer
			buffer.compact();
			int len=Math.min(buffer.remaining(),currentChunk.data.length-offset);
			buffer.put(currentChunk.data,offset,len);
			buffer.flip();
			offset+=len;
			//check if the chunk has been fully read
			if(offset>=currentChunk.data.length){
				currentChunk=null;
				offset=0;
			}
		}
		return true;
	}

	//read data from the internal buffer into target at the specified offset
	private int readFromBuffer(byte[] target, int offset){
		int available=buffer.remaining();
		int canRead=Math.min(available, target.length-offset);
		if(canRead>0){
			buffer.get(target, offset, canRead);
		}
		return canRead;
	}

	/**
	 * new application data
	 * @param data
	 * 
	 */
	protected boolean haveNewData(long sequenceNumber,byte[]data)throws IOException{
		return appData.offer(new AppData(sequenceNumber,data));
	}

	@Override
	public void close()throws IOException{
		if(closed)return;
		closed=true;
		noMoreData();
	}
	
	public UDTSocket getSocket(){
		return socket;
	}

	/**
	 * sets the blocking mode
	 * @param block
	 */
	public void setBlocking(boolean block){
		this.blocking=block;
	}
	
	/**
	 * notify the input stream that there is no more data
	 * @throws IOException
	 */
	protected void noMoreData()throws IOException{
		expectMoreData.set(false);
	}

	/**
	 * used for storing application data and the associated
	 * sequence number in the queue in ascending order
	 */
	public static class AppData implements Comparable<AppData>{
		final long sequenceNumber;
		final byte[] data;
		AppData(long sequenceNumber, byte[]data){
			this.sequenceNumber=sequenceNumber;
			this.data=data;
		}

		public int compareTo(AppData o) {
			return (int)(sequenceNumber-o.sequenceNumber);
		}

		public String toString(){
			return sequenceNumber+"["+data.length+"]";
		}
	}

}
