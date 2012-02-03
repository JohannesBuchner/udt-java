package udt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.net.InetAddress;
import java.security.MessageDigest;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Test;

import udt.util.Util;

public class TestUDTServerSocket extends UDTTestBase{
	
	boolean running=false;
	
	public int BUFSIZE=1024;
	
	int num_packets=32;

	int TIMEOUT=20000;
	
	@Test
	public void testWithoutLoss()throws Exception{
		Logger.getLogger("udt").setLevel(Level.INFO);
		UDTReceiver.dropRate=0;
		num_packets=1000;
		TIMEOUT=Integer.MAX_VALUE;
		doTest();
	}

	//set an artificial loss rate
	@Test
	public void testWithLoss()throws Exception{
		UDTReceiver.dropRate=3;
		TIMEOUT=Integer.MAX_VALUE;
		num_packets=100;
		//set log level
		Logger.getLogger("udt").setLevel(Level.INFO);
		doTest();
	}
	
	//send even more data
	@Test
	public void testLargeDataSet()throws Exception{
		UDTReceiver.dropRate=0;
		TIMEOUT=Integer.MAX_VALUE;
		num_packets=100;
		//set log level
		Logger.getLogger("udt").setLevel(Level.INFO);
		doTest();
		
	}
	
	protected void doTest()throws Exception{
		if(!running)runServer();
		UDTClient client=new UDTClient(InetAddress.getByName("localhost"),12345);
		client.connect("localhost", 65321);
		int N=num_packets*32768;
		byte[]data=new byte[N];
		new Random().nextBytes(data);
		
		while(!serverRunning)Thread.sleep(100);
		
		String md5_sent=computeMD5(data);
		long start=System.currentTimeMillis();
		System.out.println("Sending data block of <"+N/1024+"> Kbytes.");
		
		if(serverRunning){
			client.sendBlocking(data);
		}else throw new IllegalStateException();
		
		long end=System.currentTimeMillis();
		System.out.println("Shutdown client.");
		client.shutdown();
		
		while(serverRunning)Thread.sleep(100);
		
		System.out.println("Done. Sending "+N/1024+" Kbytes took "+(end-start)+" ms");
		System.out.println("Rate "+N/(end-start)+" Kbytes/sec");
		System.out.println("Server received: "+total);
		
		assertEquals(N,total);
		System.out.println("MD5 hash of data sent: "+md5_sent);
		System.out.println("MD5 hash of data received: "+md5_received);
		System.out.println(client.getStatistics());
		
		assertEquals(md5_sent,md5_received);
	}
	
	long total=0;
	
	volatile boolean serverRunning=true;
	
	volatile String md5_received=null;
	
	private void runServer()throws Exception{
		final MessageDigest md5=MessageDigest.getInstance("MD5");
		
		final UDTServerSocket serverSocket=new UDTServerSocket(InetAddress.getByName("localhost"),65321);
		
		Runnable serverProcess=new Runnable(){
			public void run(){
				try{
					System.out.println("Starting server.");
					long start=System.currentTimeMillis();
					UDTSocket s=serverSocket.accept();
					assertNotNull(s);
					UDTInputStream is=s.getInputStream();
					//is.setBlocking(false);
					byte[]buf=new byte[16384];
					while(true){
						if(checkTimeout(start))break;
						int c=is.read(buf);
						if(c<0)break;
						else{
							md5.update(buf, 0, c);
							total+=c;
							Thread.yield();
						}
					}
					System.out.println("Server thread exiting.");
					serverRunning=false;
					md5_received=Util.hexString(md5);
					serverSocket.shutDown();
					System.out.println(s.getSession().getStatistics());
				}
				catch(Exception e){
					e.printStackTrace();
					serverRunning=false;
				}
			}
		};
		Thread t=new Thread(serverProcess);
		t.start();
	}
	
	
	
	private boolean checkTimeout(long start){
		boolean to=System.currentTimeMillis()-start>TIMEOUT;
		if(to)System.out.println("TIMEOUT");
		return to;
	}
}
