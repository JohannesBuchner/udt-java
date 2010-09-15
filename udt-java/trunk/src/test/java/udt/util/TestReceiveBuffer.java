package udt.util;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;
import udt.UDTInputStream.AppData;

public class TestReceiveBuffer extends TestCase{

	public void testInOrder(){
		ReceiveBuffer b=new ReceiveBuffer(16,1);
		byte[]test1="test1".getBytes();
		byte[]test2="test2".getBytes();
		byte[]test3="test3".getBytes();
		
		b.offer(new AppData(1l,test1));
		b.offer(new AppData(2l,test2));
		b.offer(new AppData(3l,test3));
		
		AppData a=b.poll();
		assertEquals(1l,a.getSequenceNumber());
		
		a=b.poll();
		assertEquals(2l,a.getSequenceNumber());
		
		a=b.poll();
		assertEquals(3l,a.getSequenceNumber());
		
		assertNull(b.poll());
	}
	
	public void testOutOfOrder(){
		ReceiveBuffer b=new ReceiveBuffer(16,1);
		byte[]test1="test1".getBytes();
		byte[]test2="test2".getBytes();
		byte[]test3="test3".getBytes();
		
		b.offer(new AppData(3l,test3));
		b.offer(new AppData(2l,test2));
		b.offer(new AppData(1l,test1));
		
		AppData a=b.poll();
		assertEquals(1l,a.getSequenceNumber());
		
		a=b.poll();
		assertEquals(2l,a.getSequenceNumber());
		
		a=b.poll();
		assertEquals(3l,a.getSequenceNumber());
		
		assertNull(b.poll());
	}
	
	public void testInterleaved(){
		ReceiveBuffer b=new ReceiveBuffer(16,1);
		byte[]test1="test1".getBytes();
		byte[]test2="test2".getBytes();
		byte[]test3="test3".getBytes();
		
		b.offer(new AppData(3l,test3));
		
		b.offer(new AppData(1l,test1));
		
		AppData a=b.poll();
		assertEquals(1l,a.getSequenceNumber());
		
		assertNull(b.poll());
		
		b.offer(new AppData(2l,test2));
		
		a=b.poll();
		assertEquals(2l,a.getSequenceNumber());
		
		a=b.poll();
		assertEquals(3l,a.getSequenceNumber());
	}
	
	public void testOverflow(){
		ReceiveBuffer b=new ReceiveBuffer(4,1);
		
		for(int i=0; i<3; i++){
			b.offer(new AppData(i+1,"test".getBytes()));
		}
		for(int i=0; i<3; i++){
			assertEquals(i+1, b.poll().getSequenceNumber());
		}
		
		for(int i=0; i<3; i++){
			b.offer(new AppData(i+4,"test".getBytes()));
		}
		for(int i=0; i<3; i++){
			assertEquals(i+4, b.poll().getSequenceNumber());
		}
	}
	
	
	public void testTimedPoll()throws Exception{
		final ReceiveBuffer b=new ReceiveBuffer(4,1);
		
		Runnable write=new Runnable(){
			
			public void run(){
				try{
					for(int i=0; i<5; i++){
						Thread.sleep(500);
						b.offer(new AppData(i+1,"test".getBytes()));
					}
				}catch(Exception e){
					e.printStackTrace();
					fail();
				}
			}
		};
		
		Callable<String> reader=new Callable<String>(){
			public String call() throws Exception {
				for(int i=0; i<5; i++){
					AppData r=null;
					do{
						try{
							r=b.poll(200, TimeUnit.MILLISECONDS);
						}catch(InterruptedException ie){
							ie.printStackTrace();
						}
					}while(r==null);
				}
				return "OK.";
			}
		};
		
		ScheduledExecutorService es=Executors.newScheduledThreadPool(2);
		es.execute(write);
		Future<String>res=es.submit(reader);
		res.get();
		es.shutdownNow();
	}
	
}
