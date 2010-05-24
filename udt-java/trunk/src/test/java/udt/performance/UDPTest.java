package udt.performance;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;

import junit.framework.TestCase;
import udt.UDPEndPoint;
import udt.packets.DataPacket;
import udt.util.MeanValue;

/**
 * send some data over a UDP connection and measure performance
 */
public class UDPTest extends TestCase {

	final int num_packets=10*10*1000;
	final int packetSize=UDPEndPoint.DATAGRAM_SIZE;

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
		MeanValue v=new MeanValue("Datagram send time",false);
		MeanValue v2=new MeanValue("Datagram send interval",false);
		MeanValue v3=new MeanValue("Encoding time",false);
		
		for(int i=0;i<num_packets;i++){
			DataPacket p=new DataPacket();
			p.setData(data);
			v3.begin();
			dp.setData(p.getEncoded());
			v3.end();
			v2.end();
			v.begin();
			s.send(dp);
			v.end();
			v2.begin();
		}
		System.out.println("Finished sending.");
		while(serverRunning)Thread.sleep(10);
		System.out.println("Server stopped.");
		long end=System.currentTimeMillis();
		System.out.println("Done. Sending "+N/1024/1024+" Mbytes took "+(end-start)+" ms");
		System.out.println("Rate "+N/1000/(end-start)+" Mbytes/sec");
		System.out.println("Rate "+num_packets+" packets/sec");
		System.out.println("Mean send time "+v.getFormattedMean()+" microsec");
		System.out.println("Mean send interval "+v2.getFormattedMean()+" microsec");
		System.out.println("Datapacket encoding time "+v3.getFormattedMean()+" microsec");
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
					DatagramPacket dp=new DatagramPacket(buf,buf.length);
					while(true){
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
	}
	
	private final BlockingQueue<DatagramPacket> handoff=new SynchronousQueue<DatagramPacket>();
	
	private void runThirdThread()throws Exception{
		Runnable serverProcess=new Runnable(){
			public void run(){
				try{
					long start=System.currentTimeMillis();
					while(true){
						DatagramPacket dp=handoff.poll();
						if(dp!=null)total+=dp.getLength();
						if(total==N)break;
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
		
	}
	
}
