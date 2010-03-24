package udt.performance;

import java.net.InetAddress;
import java.security.MessageDigest;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import udt.UDTClient;
import udt.UDTInputStream;
import udt.UDTReceiver;
import udt.UDTServerSocket;
import udt.UDTSocket;
import udt.UDTTestBase;
import udt.util.UDTStatistics;

public class TestUDTLargeData extends UDTTestBase{
	
	boolean running=false;

	//how many
	int num_packets=200;
	//how large is a single packet
	int size=1*1024*1024;
	
	int TIMEOUT=Integer.MAX_VALUE;
	
	int READ_BUFFERSIZE=5*1024*1024;

	public void test1()throws Exception{
		Logger.getLogger("udt").setLevel(Level.INFO);
		
		UDTReceiver.dropRate=0;
		TIMEOUT=Integer.MAX_VALUE;
		UDTReceiver.connectionExpiryDisabled=true;
		doTest();
	}

	protected void doTest()throws Exception{
		if(!running)runServer();
		UDTClient client=new UDTClient(InetAddress.getByName("localhost"),12345);
		client.connect("localhost", 65321);
		
		long N=num_packets*size;
		
		byte[]data=new byte[size];
		new Random().nextBytes(data);
		
		MessageDigest digest=MessageDigest.getInstance("MD5");
		while(!serverRunning)Thread.sleep(100);
		long start=System.currentTimeMillis();
		System.out.println("Sending <"+num_packets+"> packets of <"+size/1024/1024+"> Mbytes each");
		long end=0;
		if(serverRunning){
			for(int i=0;i<num_packets;i++){
				long block=System.currentTimeMillis();
				client.sendBlocking(data);
				digest.update(data);
				System.out.println("Sent block <"+i+"> in "+(System.currentTimeMillis()-block)+" ms");
			}
			end=System.currentTimeMillis();
			client.shutdown();
		}else throw new IllegalStateException();
		String md5_sent=hexString(digest);
		
		while(serverRunning)Thread.sleep(100);
		
		System.out.println("Done. Sending "+N/1024/1024+" Mbytes took "+(end-start)+" ms");
		double mbytes=N/(end-start)/1024;
		double mbit=8*mbytes;
		System.out.println("Rate: "+(int)mbytes+" Mbytes/sec "+(int)mbit+" Mbit/sec");
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
					long start=System.currentTimeMillis();
					UDTSocket s=serverSocket.accept();
					assertNotNull(s);
					UDTInputStream is=s.getInputStream();
					byte[]buf=new byte[READ_BUFFERSIZE];
					int c=0;
					while(true){
						if(checkTimeout(start))break;
						c=is.read(buf);
						if(c<0)break;
						else{
							md5.update(buf, 0, c);
							total+=c;
							Thread.yield();
						}
					}
					System.out.println("Server thread exiting, last received bytes: "+c);
					serverRunning=false;
					md5_received=UDTStatistics.hexString(md5);
					serverSocket.shutDown();
					System.out.println(s.getSession().getStatistics());
				}
				catch(Exception e){
					e.printStackTrace();
					fail();
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
