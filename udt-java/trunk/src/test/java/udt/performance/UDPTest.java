package udt.performance;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Random;

import junit.framework.TestCase;

/**
 * send some data over a UDP connection and measure performance
 * 
 */
public class UDPTest extends TestCase {

	final int BUFSIZE=32768;
	final int num_packets=10*1000;
	final int packetSize=1024;
	
	public void test1()throws Exception{
		runServer();
		//client socket
		DatagramSocket s=new DatagramSocket(12345);

		//generate a test array with random content
		N=num_packets*packetSize;
		byte[]data=new byte[N];
		new Random().nextBytes(data);
		long start=System.currentTimeMillis();
		ByteBuffer bb=ByteBuffer.wrap(data);
		DatagramPacket dp=new DatagramPacket(new byte[BUFSIZE],BUFSIZE);
		dp.setAddress(InetAddress.getByName("localhost"));
		dp.setPort(65321);
		
		System.out.println("Sending data block of <"+N+"> bytes");
		while(bb.remaining()>0){
			int len=Math.min(bb.remaining(),BUFSIZE);
			byte[]chunk=new byte[len];
			bb.get(chunk);
			dp.setData(chunk);
			s.send(dp);
		}
		System.out.println("Finished sending.");
		while(serverRunning)Thread.sleep(10);
		System.out.println("Server stopped.");
		long end=System.currentTimeMillis();
		System.out.println("Done. Sending "+N/1024/1024+" Mbytes took "+(end-start)+" ms");
		System.out.println("Rate "+N/(end-start)+" Kbytes/sec");
		System.out.println("Server received: "+total);
	}
	
	int N=0;
	long total=0;
	volatile boolean serverRunning=true;
	
	private void runServer()throws Exception{
		//server socket
		final DatagramSocket serverSocket=new DatagramSocket(65321);

		Runnable serverProcess=new Runnable(){
			public void run(){
				try{
					byte[]buf=new byte[BUFSIZE];
					DatagramPacket dp=new DatagramPacket(buf,buf.length);
					while(true){
						serverSocket.receive(dp);
						total+=dp.getLength();
						if(total==N)break;
					}
				}
				catch(Exception e){
					e.printStackTrace();
				}
				serverRunning=false;
			}
		};
		Thread t=new Thread(serverProcess);
		t.start();
	}
}
