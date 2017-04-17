package udt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Test;

import udt.packets.Destination;

public class TestUdpEndpoint extends UDTTestBase{

	@Test
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
	@Test
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
	
	//@Test()
	public void testRendezvousConnect()throws Exception{
	
	}
	
	@Test
	public void testBindToAnyPort()throws Exception{
		UDPEndPoint ep=new UDPEndPoint(InetAddress.getByName("localhost"));
		int port=ep.getLocalPort();
		assertTrue(port>0);
	}
	
}
