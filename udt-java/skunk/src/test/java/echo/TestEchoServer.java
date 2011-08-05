package echo;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;

import junit.framework.TestCase;
import udt.UDTClient;
import udt.UDTInputStream;
import udt.util.Util;

public class TestEchoServer extends TestCase {

	public void test1()throws Exception{
		EchoServer es=new EchoServer(65321);
		es.start();
		Thread.sleep(1000);
		UDTClient client=new UDTClient(InetAddress.getByName("localhost"),12345);
		client.connect("localhost", 65321);
		PrintWriter pw=new PrintWriter(new OutputStreamWriter(client.getOutputStream(),"UTF-8"));
		pw.println("test");
		pw.flush();
		System.out.println("Message sent.");
                // TODO: Need to change UDTInputStream to use wait(timeout)
		((UDTInputStream) client.getInputStream()).setBlocking(false);
		String line=Util.readLine(client.getInputStream());
		assertNotNull(line);
		System.out.println(line);
		assertEquals("test",line);
	}

}
