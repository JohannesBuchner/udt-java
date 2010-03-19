package udt.util;

import java.io.File;
import java.io.FileInputStream;
import java.net.InetAddress;
import java.nio.ByteBuffer;

import udt.UDTInputStream;
import udt.UDTOutputStream;
import udt.UDTServerSocket;
import udt.UDTSocket;
import udt.packets.PacketUtil;

/**
 * helper application for sending a single file via UDT
 * Intended to be compatible with the C++ version in 
 * the UDT reference implementation
 * 
 * main method USAGE: java -cp .. udt.util.SendFile <server_port>
 */
public class SendFile implements Runnable{

	private final int serverPort;

	public SendFile(int serverPort){
		this.serverPort=serverPort;
	}

	public void run(){
		try{
			UDTServerSocket server=new UDTServerSocket(InetAddress.getByName("localhost"),serverPort);
			UDTSocket socket=server.accept();
			UDTInputStream in=socket.getInputStream();
			UDTOutputStream out=socket.getOutputStream();
			byte[]readBuf=new byte[32768];
			ByteBuffer bb=ByteBuffer.wrap(readBuf);

			//read name file info 
			in.read(readBuf);

			//how many bytes to read for the file name
			int length=bb.getInt();
			byte[]fileName=new byte[length];
			bb.get(fileName);

			File file=new File(new String(fileName));
			System.out.println("File requested: "+file.getPath());

			FileInputStream fis=new FileInputStream(file);
			try{
				long size=file.length();

				PacketUtil.encode(size);
				//send size info
				out.write(PacketUtil.encode(size));
				long start=System.currentTimeMillis();
				//and send the file
				Util.copy(fis, out, size);
				long end=System.currentTimeMillis();
				System.out.println(socket.getSession().getStatistics());
				System.out.println("Rate: "+1000*size/1024/1024/(end-start)+" MBytes/sec.");
			}finally{
				fis.close();
			}
		}catch(Exception ex){
			throw new RuntimeException(ex);
		}
	}

	/**
	 * main() method for invoking as a commandline application
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception{
		int serverPort=65321;
		try{
			serverPort=Integer.parseInt(args[0]);
		}catch(Exception ex){
			usage();
			System.exit(1);
		}
		SendFile sf=new SendFile(serverPort);
		sf.run();
	}

	public static void usage(){
		System.out.println("Usage: java -cp ... udt.util.SendFile <server_port>");
	}

}
