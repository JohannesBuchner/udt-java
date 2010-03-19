package udt.util;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class UDTThreadFactory implements ThreadFactory {

	private static final AtomicInteger num=new AtomicInteger(0);
	
	private static UDTThreadFactory theInstance=null;
	
	public static synchronized UDTThreadFactory get(){
		if(theInstance==null)theInstance=new UDTThreadFactory();
		return theInstance;
	}
	
	public Thread newThread(Runnable r) {
		Thread t=new Thread(r);
		t.setName("UDT-Thread-"+num.incrementAndGet());
		return t;
	}

}
