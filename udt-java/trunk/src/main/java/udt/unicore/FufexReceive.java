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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.nio.ByteBuffer;

import udt.UDTClient;
import udt.util.Util;

/**
 * Receive part of the UNICORE integration code.
 *  
 * usage: recvfile server_ip local_filename mode [comm_out comm_in]
 * 
 * 
 */
public class FufexReceive implements Runnable {

	private final String serverIP;
	private final String localFile;
	private final boolean append;
	private final String commOut;
	private final String commIn;

	public FufexReceive(String serverIP, String localFile, boolean append, String commOut, String commIn){
		this.serverIP=serverIP;
		this.localFile=localFile;
		this.append=append;
		this.commIn=commIn;
		this.commOut=commOut;
	}

	public void run(){
		try{
			//open the UDPEndpoint
			UDTClient client=new UDTClient(InetAddress.getLocalHost(),0);
			int localPort=client.getEndpoint().getLocalPort();
			//write the port to output
			writeToOut("OUT: "+localPort);
			
			//read peer port from input file or stdin
			String peerPortS=readFromIn();
			int serverPort=Integer.parseInt(peerPortS);
			
			//connect...
			client.connect(serverIP,serverPort);
			InputStream in=client.getInputStream();
			
			//read file size info (an 4-byte int) 
			byte[]sizeInfo=new byte[4];
			in.read(sizeInfo);
			long size=ByteBuffer.wrap(sizeInfo).getInt();
			
			//now read file data
			FileOutputStream fos=new FileOutputStream(localFile,append);
			try{
				Util.copy(in, fos, size, false);
			}finally{
				fos.close();
			}
			
			
		}catch(Exception ex){
			throw new RuntimeException(ex);
		}
	}

	public static void main(String[] args)throws Exception{
		if(args.length<3)usage();
		
		String serverIP=args[0];
		String localFile=args[1];
		boolean append=args[2].equals("A");
		String commIn=null;
		String commOut=null;
		if(args.length>3){
			commOut=args[3];
			commIn=args[4];
		}
		FufexReceive fr=new FufexReceive(serverIP,localFile,append,commOut,commIn);
		fr.run();
	}

	//print usage info and exit with error
	private static void usage(){
		System.err.println("usage: recvfile server_ip local_filename mode [comm_out comm_in]");
		System.exit(1);
	}

	private String readFromIn()throws IOException, InterruptedException{
		InputStream in=System.in;
		if(commIn!=null){
			File file=new File(commIn);
			while(!file.exists()){
				Thread.sleep(2000);
			}
			in=new FileInputStream(file);
		}
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		try{
			return br.readLine();
		}finally{
			if(commIn!=null)in.close();//do not close System.in
		}
	}
	
	
	private void writeToOut(String line)throws IOException{
		if(commOut!=null){
			appendToFile(commOut, line);
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
