/*********************************************************************************
 * Copyright (c) 2010 Forschungszentrum Juelich GmbH 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * (1) Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the disclaimer at the end. Redistributions in
 * binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution.
 * 
 * (2) Neither the name of Forschungszentrum Juelich GmbH nor the names of its 
 * contributors may be used to endorse or promote products derived from this 
 * software without specific prior written permission.
 * 
 * DISCLAIMER
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *********************************************************************************/

package udt.unicore;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;

import udt.UDTOutputStream;
import udt.UDTServerSocket;
import udt.UDTSocket;
import udt.packets.PacketUtil;
import udt.util.Util;

/**
 * This commandline application is run on the target system to
 * send a remote file. <br/>
 * 
 * Performs UDP hole punching, waits for a client connect and
 * sends the specified file. <br/>
 * 
 * usage: sendfile client_ip client_port local_filename [comm_file_name]
 *
 * 
 */
public class FufexSend {

	private final String clientIP;
	private final int clientPort;
	private final String localFilename;
	private final String commFilename;
	
	public FufexSend(String clientIP, int clientPort, String localFilename, String commFilename){
		this.clientIP=clientIP;
		this.clientPort=clientPort;
		this.localFilename=localFilename;
		this.commFilename=commFilename;
	}
	
	public void run(){
		try{
			//create an UDTServerSocket on a free port
			UDTServerSocket server=new UDTServerSocket(0);
			
			// do hole punching to allow the client to connect
			InetAddress clientAddress=InetAddress.getByName(clientIP);
			Util.doHolePunch(server.getEndpoint(),clientAddress, clientPort);
			int localPort=server.getEndpoint().getLocalPort();
			//output client port
			writeToOut("OUT: "+localPort);
			
			//now start the send...
			UDTSocket socket=server.accept();
			UDTOutputStream out=socket.getOutputStream();
			File file=new File(localFilename);
			FileInputStream fis=new FileInputStream(file);
			try{
				//send file size info
				long size=file.length();
				PacketUtil.encode(size);
				out.write(PacketUtil.encode(size));
				long start=System.currentTimeMillis();
				//and send the file
				Util.copy(fis, out, size,true);
				long end=System.currentTimeMillis();
				System.out.println(socket.getSession().getStatistics());
				float mbRate=1000*size/1024/1024/(end-start);
				float mbitRate=8*mbRate;
				System.out.println("Rate: "+(int)mbRate+" MBytes/sec. "+mbitRate+" mbit/sec.");
			}finally{
				fis.close();
			}
		}catch(Exception ex){
			throw new RuntimeException(ex);
		}
	}
	
	
	//print usage info and exit with error
	private static void usage(){
		System.err.println("usage: send client_ip client_port local_filename [comm_file_name]");
		System.exit(1);
	}

	public static void main(String[] args)throws Exception{
		if(args.length<3)usage();
		
		String commFileName=null;
		if(args.length>3){
			commFileName=args[3];
		}

		String clientIP=args[0];
		int clientPort=Integer.parseInt(args[1]);
		
		FufexSend fs=new FufexSend(clientIP,clientPort,args[2],commFileName);
		fs.run();
	}

	private void writeToOut(String line)throws IOException{
		if(commFilename!=null){
			appendToFile(commFilename, line);
		}
		else{
			System.out.println(line);
		}
	}
	
	/**
	 * append a line to the named file (and a newline character)
	 * @param name - the file to write to
	 * @param line - the line to write
	 */
	private void appendToFile(String name, String line) throws IOException{
		File f=new File(name);
		FileOutputStream fos=new FileOutputStream(f,true);
		try{
			fos.write(line.getBytes());
			fos.write('\n');
		}
		finally{
			fos.close();
		}
	}
	
}
