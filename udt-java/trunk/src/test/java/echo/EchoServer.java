package echo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;

import udt.UDTInputStream;
import udt.UDTOutputStream;
import udt.UDTServerSocket;
import udt.UDTSocket;
import udt.util.UDTThreadFactory;

public class EchoServer implements Runnable{

	
	final UDTServerSocket server;
	final Thread serverThread;
	
	volatile boolean started=false;
	
	public EchoServer(int port)throws Exception{
		server=new UDTServerSocket(InetAddress.getByName("localhost"),port);
		serverThread=UDTThreadFactory.get().newThread(this);
	}
	
	public void start(){
		serverThread.start();
	}
	
	public void run(){
		try{
			started=true;
			UDTSocket socket=server.accept();
			System.out.println("Client CONNECTED");
			UDTInputStream in=socket.getInputStream();
			UDTOutputStream out=socket.getOutputStream();
			PrintWriter writer=new PrintWriter(new OutputStreamWriter(out));
			while(true){
				String line=readLine(in);
				if(line==null)break;
				System.out.println("ECHO: "+line);
				//else echo back the line
				writer.println(line);
				writer.flush();
			}
			
		}catch(Exception ex){
			ex.printStackTrace();
		}
		
	}
	
	String readLine(InputStream r)throws IOException{
		ByteArrayOutputStream bos=new ByteArrayOutputStream();
		while(true){
			int c=r.read();
			if(c<0 && bos.size()==0)return null;
			if(c<0 || c==10)break;
			else bos.write(c);
		}
		return bos.toString();
	}
	
}
