package echo;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;

import junit.framework.TestCase;
import udt.UDTClient;
import udt.util.Util;

public class TestEchoServerMultiClient extends TestCase {

	public void testTwoClients()throws Exception{
		EchoServer es=new EchoServer(65321);
		es.start();
		Thread.sleep(1000);
		
		UDTClient client=new UDTClient(InetAddress.getByName("localhost"),12345);
		client.connect("localhost", 65321);
		
		UDTClient client2=new UDTClient(InetAddress.getByName("localhost"),12346);
		client2.connect("localhost", 65321);
		
		doClientCommunication(client);
		doClientCommunication(client);
	}

	private void doClientCommunication(UDTClient client)throws Exception{
		PrintWriter pw=new PrintWriter(new OutputStreamWriter(client.getOutputStream(),"UTF-8"));
		pw.println("test");
		pw.flush();
		System.out.println("Message sent.");
		client.getInputStream().setBlocking(false);
		String line=Util.readLine(client.getInputStream());
		assertNotNull(line);
		System.out.println(line);
		assertEquals("test",line);
	}
}
