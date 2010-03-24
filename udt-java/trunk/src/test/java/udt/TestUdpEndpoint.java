package udt;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

import udt.packets.Destination;

public class TestUdpEndpoint extends UDTTestBase{

	public void testClientServerMode()throws Exception{

		//select log level
		Logger.getLogger("udt").setLevel(Level.INFO);
		
		UDPEndPoint server=new UDPEndPoint(InetAddress.getByName("localhost"),65322);
		server.start();
		UDTClient client=new UDTClient(InetAddress.getByName("localhost"),12346);
		client.connect("localhost", 65322);
		
		//test a large message (resulting in multiple data packets)
		int num_packets=100;
		int N=num_packets*1024;
		byte[]data=getRandomData(N);
		
		client.sendBlocking(data);
		Thread.sleep(2000);
		System.out.println(client.getStatistics());
		System.out.println(server.getSessions().iterator().next().getStatistics());
		int sent=client.getStatistics().getNumberOfSentDataPackets();
		int received=server.getSessions().iterator().next().getStatistics().getNumberOfReceivedDataPackets();
		assertEquals(sent, received);
		
		server.stop();
		Thread.sleep(2000);
	}	
	
	
	/**
	 * just check how fast we can send out UDP packets from the endpoint
	 * @throws Exception
	 */
	public void testRawSendRate()throws Exception{
		Logger.getLogger("udt").setLevel(Level.WARNING);
		System.out.println("Checking raw UDP send rate...");
		InetAddress localhost=InetAddress.getByName("localhost");
		UDPEndPoint endpoint=new UDPEndPoint(localhost,65322);
		endpoint.start();
		Destination d1=new Destination(localhost,12345);
		int dataSize=UDTSession.DEFAULT_DATAGRAM_SIZE;
		DatagramPacket p=new DatagramPacket(getRandomData(dataSize),dataSize,d1.getAddress(),d1.getPort());
		int N=100000;
		long start=System.currentTimeMillis();
		//send many packets as fast as we can
		for(int i=0;i<N;i++){
			endpoint.sendRaw(p);
		}
		long end=System.currentTimeMillis();
		float rate=1000*N/(end-start);
		System.out.println("PacketRate: "+(int)rate+" packets/sec.");
		float dataRate=dataSize*rate/1024/1024;
		System.out.println("Data Rate:  "+(int)dataRate+" MBytes/sec.");
		endpoint.stop();
		Thread.sleep(1000);
	}
	
	public void testRendezvousConnect()throws Exception{
		final UDTClient c1=new UDTClient(InetAddress.getByName("localhost"),12345);
		final UDTClient c2=new UDTClient(InetAddress.getByName("localhost"),34567);
		final String testMsg="test!!";
	
		Runnable r1=new Runnable(){
			public void run(){
				try{
					//connect first client to second one
					c1.connect("localhost", 34567);
					System.out.println("C1 connected");
					//send a message
					c1.getOutputStream().write(testMsg.getBytes());
					c1.flush();
					//read a message back
					byte[]buf=new byte[testMsg.length()];
					c1.getInputStream().read(buf);
					String received=new String(buf);
					assertEquals(testMsg, received);
					
				}catch(Exception ex){
					ex.printStackTrace();
				}
			}
		};
		Thread t1=new Thread(r1);
		t1.start();
		
		//connect second client to first one
		c2.connect("localhost", 12345);
		System.out.println("C2 connected.");
		Thread.sleep(500);
		byte[]buf=new byte[testMsg.length()];
		c2.getInputStream().read(buf);
		String received=new String(buf);
		assertEquals(testMsg, received);
		//send a message back
		c2.getOutputStream().write(testMsg.getBytes());
		c2.flush();
		
		//stop endpoints
		c1.getEndpoint().stop();
		c2.getEndpoint().stop();
	}
	
	public void testBindToAnyPort()throws Exception{
		UDPEndPoint ep=new UDPEndPoint(InetAddress.getByName("localhost"));
		int port=ep.getLocalPort();
		assertTrue(port>0);
	}
	
}
