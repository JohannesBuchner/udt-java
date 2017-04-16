package echo;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;

import junit.framework.Assert;

import org.junit.Test;

import udt.UDTClient;
import udt.util.Util;

public class TestEchoServer {

	@Test
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
		client.getInputStream().setBlocking(false);
		String line=Util.readLine(client.getInputStream());
		Assert.assertNotNull(line);
		System.out.println(line);
		Assert.assertEquals("test",line);
	}

}
