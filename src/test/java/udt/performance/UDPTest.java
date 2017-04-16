package udt.performance;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import udt.UDPEndPoint;
import udt.packets.DataPacket;
import udt.util.MeanValue;

/**
 * send some data over a UDP connection and measure performance
 */
public class UDPTest {

	final int num_packets=1000;
	final int packetSize=UDPEndPoint.DATAGRAM_SIZE;

	@Test
	public void test1()throws Exception{
		runServer();
		runThirdThread();
		
		//client socket
		DatagramSocket s=new DatagramSocket(12345);
		
		//generate a test array with random content
		N=num_packets*packetSize;
		byte[]data=new byte[packetSize];
		new Random().nextBytes(data);
		long start=System.currentTimeMillis();
		DatagramPacket dp=new DatagramPacket(new byte[packetSize],packetSize);
		dp.setAddress(InetAddress.getByName("localhost"));
		dp.setPort(65321);
		System.out.println("Sending "+num_packets+" data blocks of <"+packetSize+"> bytes");
		MeanValue dgSendTime=new MeanValue("Datagram send time",false);
		MeanValue dgSendInterval=new MeanValue("Datagram send interval",false);
		
		for(int i=0;i<num_packets;i++){
			DataPacket p=new DataPacket();
			p.setData(data);
			dp.setData(p.getEncoded());
			dgSendInterval.end();
			dgSendTime.begin();
			s.send(dp);
			Thread.sleep(5);
			dgSendTime.end();
			dgSendInterval.begin();
		}
		System.out.println("Finished sending.");
		while(serverRunning)Thread.sleep(10);
		System.out.println("Server stopped.");
		long end=System.currentTimeMillis();
		System.out.println("Done. Sending "+N/1024/1024+" Mbytes took "+(end-start)+" ms");
		float rate=N/1000/(end-start);
		System.out.println("Rate "+rate+" Mbytes/sec "+(rate*8)+ " Mbit/sec");
		System.out.println("Rate "+num_packets+" packets/sec");
		System.out.println("Mean send time "+dgSendTime.get());
		System.out.println("Mean send interval "+dgSendInterval.get());
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
					byte[]buf=new byte[packetSize];
					while(true){
						DatagramPacket dp=new DatagramPacket(buf,buf.length);
						serverSocket.receive(dp);
						handoff.offer(dp);
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
		System.out.println("Server started.");
	}
	
	private final BlockingQueue<DatagramPacket> handoff=new SynchronousQueue<DatagramPacket>();
	
	private void runThirdThread()throws Exception{
		Runnable serverProcess=new Runnable(){
			public void run(){
				try{
					int counter=0;
					long start=System.currentTimeMillis();
					while(counter<num_packets){
						DatagramPacket dp=handoff.poll(10, TimeUnit.MILLISECONDS);
						if(dp!=null){
							total+=dp.getLength();
							counter++;
							System.out.println("Count: "+counter);
						}
					}
					long end=System.currentTimeMillis();
					System.out.println("Server time: "+(end-start)+" ms.");

				}
				catch(Exception e){
					e.printStackTrace();
				}
				serverRunning=false;
			}
		};
		Thread t=new Thread(serverProcess);
		t.start();
		System.out.println("Hand-off thread started.");
		
	}

}
