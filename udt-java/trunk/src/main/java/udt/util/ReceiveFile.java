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

package udt.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.text.NumberFormat;

import udt.UDTClient;
import udt.UDTInputStream;
import udt.UDTOutputStream;
import udt.UDTReceiver;

/**
 * helper class for receiving a single file via UDT
 * Intended to be compatible with the C++ version in 
 * the UDT reference implementation
 * 
 * main method USAGE: 
 * java -cp ... udt.util.ReceiveFile <server_ip> <server_port> <remote_filename> <local_filename>
 */
public class ReceiveFile extends Application{

	private final int serverPort;
	private final String serverHost;
	private final String remoteFile;
	private final String localFile;
	private final NumberFormat format;
	
	public ReceiveFile(String serverHost, int serverPort, String remoteFile, String localFile){
		this.serverHost=serverHost;
		this.serverPort=serverPort;
		this.remoteFile=remoteFile;
		this.localFile=localFile;
		format=NumberFormat.getNumberInstance();
		format.setMaximumFractionDigits(3);
	}
	
	public void run(){
		configure();
		verbose=true;
		try{
			UDTReceiver.connectionExpiryDisabled=true;
			InetAddress myHost=localIP!=null?InetAddress.getByName(localIP):InetAddress.getLocalHost();
			UDTClient client=localPort!=-1?new UDTClient(myHost,localPort):new UDTClient(myHost);
			client.connect(serverHost, serverPort);
			UDTInputStream in=client.getInputStream();
			UDTOutputStream out=client.getOutputStream();
			
			System.out.println("[ReceiveFile] Requesting file "+remoteFile);
			byte[]fName=remoteFile.getBytes();
			
			//send file name info
			byte[]nameinfo=new byte[fName.length+4];
			System.arraycopy(encode(fName.length), 0, nameinfo, 0, 4);
			System.arraycopy(fName, 0, nameinfo, 4, fName.length);
			
			out.write(nameinfo);
			out.flush();
			//pause the sender to save some CPU time
			out.pauseOutput();
			
			//read size info (an 64 bit number) 
			byte[]sizeInfo=new byte[8];
			
			int total=0;
			while(total<sizeInfo.length){
				int r=in.read(sizeInfo);
				if(r<0)break;
				total+=r;
			}
			long size=decode(sizeInfo, 0);
			
			File file=new File(new String(localFile));
			System.out.println("[ReceiveFile] Write to local file <"+file.getAbsolutePath()+">");
			FileOutputStream fos=new FileOutputStream(file);
			OutputStream os=new BufferedOutputStream(fos,1024*1024);
			try{
				System.out.println("[ReceiveFile] Reading <"+size+"> bytes.");
				long start = System.currentTimeMillis();
			    //and read the file data
				Util.copy(in, os, size, false);
				long end = System.currentTimeMillis();
				double rate=1000.0*size/1024/1024/(end-start);
				System.out.println("[ReceiveFile] Rate: "+format.format(rate)+" MBytes/sec. "
						+format.format(8*rate)+" MBit/sec.");
			
				client.shutdown();
				
				if(verbose)System.out.println(client.getStatistics());
				
			}finally{
				fos.close();
			}		
		}catch(Exception ex){
			throw new RuntimeException(ex);
		}
	}
	
	
	public static void main(String[] fullArgs) throws Exception{
		int serverPort=65321;
		String serverHost="localhost";
		String remoteFile="";
		String localFile="";
		
		String[] args=parseOptions(fullArgs);
		
		try{
			serverHost=args[0];
			serverPort=Integer.parseInt(args[1]);
			remoteFile=args[2];
			localFile=args[3];
		}catch(Exception ex){
			usage();
			System.exit(1);
		}
		
		ReceiveFile rf=new ReceiveFile(serverHost,serverPort,remoteFile, localFile);
		rf.run();
	}
	
	public static void usage(){
		System.out.println("Usage: java -cp .. udt.util.ReceiveFile " +
				"<server_ip> <server_port> <remote_filename> <local_filename> " +
				"[--verbose] [--localPort=<port>] [--localIP=<ip>]");
	}
	
}
