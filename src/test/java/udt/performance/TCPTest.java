package udt.performance;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;

import junit.framework.TestCase;

/**
 * send some data over a TCP connection and measure performance
 * 
 */
public class TCPTest extends TestCase {
	
	int BUFSIZE=1024;
	int num_packets=10*1000;

	public void test1()throws Exception{
		runServer();
		//client socket
		Socket s=new Socket("localhost",65321);
		OutputStream os=s.getOutputStream();
		int N=num_packets*1024;
		byte[]data=new byte[N];
		new Random().nextBytes(data);
		long start=System.currentTimeMillis();
	
		System.out.println("Sending data block of <"+N+"> bytes.");
		os.write(data);
		os.flush();
		os.close();
		while(serverRunning)Thread.sleep(10);
		long end=System.currentTimeMillis();
		System.out.println("Done. Sending "+N/1024+" Kbytes took "+(end-start)+" ms");
		System.out.println("Rate "+N/(end-start)+" Kbytes/sec");
		System.out.println("Server received: "+total);
	}
	
	long total=0;
	volatile boolean serverRunning=true;
	
	private void runServer()throws Exception{
		//server socket
		final ServerSocket serverSocket=new ServerSocket(65321);
		Runnable serverProcess=new Runnable(){
			public void run(){
				try{
					Socket s=serverSocket.accept();
					InputStream is=s.getInputStream();
					byte[]buf=new byte[16384];
					while(true){
						int c=is.read(buf);
						if(c<0)break;
						total+=c;
					}
					serverRunning=false;
				}
				catch(Exception e){
					e.printStackTrace();
					fail();
				}
			}
		};
		Thread t=new Thread(serverProcess);
		t.start();
	}
}
