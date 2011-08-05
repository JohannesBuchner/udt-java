package udt.sender;

import java.util.concurrent.locks.ReentrantLock;

import udt.packets.DataPacket;

/**
 * 
 * holds a fixed number of {@link DataPacket} instances which are sent out.
 * 
 * it is assumed that a single thread stores new data, and another single thread
 * reads/removes data
 *
 * @author schuller
 */
public class FlowWindow {

	private final DataPacket[]packets;

	private final int length;

	private volatile boolean isEmpty=true;

	private volatile boolean isFull=false;

	private volatile int validEntries=0;

	private volatile boolean isCheckout=false;

	private volatile int writePos=0;

	private volatile int readPos=-1;

	private volatile int consumed=0;

	private volatile int produced=0;

	private final ReentrantLock lock;

	/**
	 * @param size - flow window size
	 * @param chunksize - data chunk size
	 */
	public FlowWindow(int size, int chunksize){
		this.length=size;
		packets=new DataPacket[length];
		for(int i=0;i<packets.length;i++){
			packets[i]=new DataPacket();
			packets[i].setData(new byte[chunksize]);
		}
		lock=new ReentrantLock(true);
	}

	/**
	 * get a data packet for updating with new data
	 * 
	 * @return <code>null</code> if flow window is full
	 */
	public DataPacket getForProducer(){
		lock.lock();
		try{
			if(isFull){
				return null;
			}
			if(isCheckout)throw new IllegalStateException();
			isCheckout=true;
			DataPacket p=packets[writePos];
			return p;
		}finally{
			lock.unlock();
		}
	}

	public void produce(){
		lock.lock();
		try{
			isCheckout=false;
			writePos++;
			if(writePos==length)writePos=0;
			validEntries++;
			isFull=validEntries==length-1;
			isEmpty=false;
			produced++;
		}finally{
			lock.unlock();
		}
	}


	public DataPacket consumeData(){
		if(isEmpty){
			return null;
		}
		lock.lock();
		try{
			readPos++;
			DataPacket p=packets[readPos];
			if(readPos==length-1)readPos=-1;
			validEntries--;
			isEmpty=validEntries==0;
			isFull=false;
			consumed++;
			return p;
		}finally{
			lock.unlock();
		}
	}

	boolean isEmpty(){
		return isEmpty;
	}

	/**
	 * check if another entry can be added
	 * @return
	 */
	public boolean isFull(){
		return isFull;
	}

	int readPos(){
		return readPos;
	}

	int writePos(){
		return writePos;
	}
	
	int consumed(){
		return consumed;
	}
	public String toString(){
		StringBuilder sb=new StringBuilder();
		sb.append("FlowWindow size=").append(length);
		sb.append(" full=").append(isFull).append(" empty=").append(isEmpty);
		sb.append(" consumed=").append(consumed).append(" produced=").append(produced);
		return sb.toString();
	}
}
