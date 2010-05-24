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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.security.MessageDigest;

import udt.UDPEndPoint;

/**
 * helper methods 
 */
public class Util {

	/**
	 * get the current timer value in microseconds
	 * @return
	 */
	public static long getCurrentTime(){
		return System.nanoTime()/1000;
	}
	
	
	public static final long SYN=10000;
	
	public static final double SYN_D=10000.0;
	
	/**
	 * get the SYN time in microseconds. The SYN time is 0.01 seconds = 10000 microseconds
	 * @return
	 */
	public static final long getSYNTime(){
		return 10000;
	}
	
	public static double getSYNTimeD(){
		return 10000.0;
	}
	
	/**
	 * get the SYN time in seconds. The SYN time is 0.01 seconds = 10000 microseconds
	 * @return
	 */
	public static double getSYNTimeSeconds(){
		return 0.01;
	}
	/**
	 * read a line terminated by a new line '\n' character
	 * @param input - the input string to read from 
	 * @return the line read or <code>null</code> if end of input is reached
	 * @throws IOException
	 */
	public static String readLine(InputStream input)throws IOException{
		return readLine(input, '\n');
	}
	
	/**
	 * read a line from the given input stream
	 * @param input - the input stream
	 * @param terminatorChar - the character used to terminate lines
	 * @return the line read or <code>null</code> if end of input is reached
	 * @throws IOException
	 */
	public static String readLine(InputStream input, char terminatorChar)throws IOException{
		ByteArrayOutputStream bos=new ByteArrayOutputStream();
		while(true){
			int c=input.read();
			if(c<0 && bos.size()==0)return null;
			if(c<0 || c==terminatorChar)break;
			else bos.write(c);
		}
		return bos.size()>0? bos.toString() : null;
	}

	/**
	 * copy input data from the source stream to the target stream
	 * @param source - input stream to read from
	 * @param target - output stream to write to
	 * @throws IOException
	 */
	
	public static void copy(InputStream source, OutputStream target)throws Exception{
		copy(source,target,-1, false);
	}
	
	/**
	 * copy input data from the source stream to the target stream
	 * @param source - input stream to read from
	 * @param target - output stream to write to
	 * @param size - how many bytes to copy (-1 for no limit)
	 * @param flush - whether to flush after each write
	 * @throws IOException
	 */
	public static void copy(InputStream source, OutputStream target, long size, boolean flush)throws IOException{
		byte[]buf=new byte[8*65536];
		int c;
		long read=0;
		while(true){
			c=source.read(buf);
			if(c<0)break;
			read+=c;
			target.write(buf, 0, c);
			if(flush)target.flush();
			if(size>0 && read>=size)break;
		}
		if(!flush)target.flush();
	}
	
	/**
	 * perform UDP hole punching to the specified client by sending 
	 * a dummy packet. A local port will be chosen automatically.
	 * 
	 * @param client - client address
	 * @return the local port that can now be accessed by the client
	 * @throws IOException
	 */
	public static void doHolePunch(UDPEndPoint endpoint,InetAddress client, int clientPort)throws IOException{
		DatagramPacket p=new DatagramPacket(new byte[1],1);
		p.setAddress(client);
		p.setPort(clientPort);
		endpoint.sendRaw(p);
	}

	public static String hexString(MessageDigest digest){
		byte[] messageDigest = digest.digest();
		StringBuilder hexString = new StringBuilder();
		for (int i=0;i<messageDigest.length;i++) {
			String hex = Integer.toHexString(0xFF & messageDigest[i]); 
			if(hex.length()==1)hexString.append('0');
			hexString.append(hex);
		}
		return hexString.toString();
	}
	
}
