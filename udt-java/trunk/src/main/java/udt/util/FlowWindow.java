package udt.util;

import java.util.concurrent.PriorityBlockingQueue;

/**
 * bounded queue 
 * 
 */
public class FlowWindow<E> extends PriorityBlockingQueue<E> {

	private static final long serialVersionUID=1l;

	private volatile int capacity;
	
	/**
	 * create a new flow window with the given size
	 * 
	 * @param size - the initial size of the flow window
	 */
	public FlowWindow(int size){
		super();
		this.capacity=size;
	}
	
	/**
	 * create a new flow window with the default size of 16
	 */
	public FlowWindow(){
		this(16);
	}
	
	public void setCapacity(int newSize){
		capacity=newSize;
	}
	
	public int getCapacity(){
		return capacity;
	}

	/**
	 * try to add an element to the queue, return false if it is not possible
	 */
	@Override
	public boolean offer(E e) {
		if(size()<capacity){
			return super.offer(e);
		}else return false;
	}	
	
	
	
}
