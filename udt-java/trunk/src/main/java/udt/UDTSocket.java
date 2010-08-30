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

package udt;
import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

import udt.packets.DataPacket;

/**
 * UDTSocket is analogous to a normal java.net.Socket, it provides input and 
 * output streams for the application
 * 
 * TODO is it possible to actually extend java.net.Socket ?
 * 
 * 
 */
public class UDTSocket {
	
	//endpoint
	private final UDPEndPoint endpoint;
	
	private volatile boolean active;
	
    //processing received data
	private UDTReceiver receiver;
	private UDTSender sender;
	
	private final UDTSession session;

	private UDTInputStream inputStream;
	private UDTOutputStream outputStream;

	/**
     * @param host
     * @param port
     * @param endpoint
     * @throws SocketException,UnknownHostException
     */
	public UDTSocket(UDPEndPoint endpoint, UDTSession session)throws SocketException,UnknownHostException{
		this.endpoint=endpoint;
		this.session=session;
		this.receiver=new UDTReceiver(session,endpoint);
		this.sender=new UDTSender(session,endpoint);
	}
	
	public UDTReceiver getReceiver() {
		return receiver;
	}

	public void setReceiver(UDTReceiver receiver) {
		this.receiver = receiver;
	}

	public UDTSender getSender() {
		return sender;
	}

	public void setSender(UDTSender sender) {
		this.sender = sender;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public boolean isActive() {
		return active;
	}

	public UDPEndPoint getEndpoint() {
		return endpoint;
	}

	/**
	 * get the input stream for reading from this socket
	 * @return
	 */
	public synchronized UDTInputStream getInputStream()throws IOException{
		if(inputStream==null){
			inputStream=new UDTInputStream(this);
		}
		return inputStream;
	}
    
	/**
	 * get the output stream for writing to this socket
	 * @return
	 */
	public synchronized UDTOutputStream getOutputStream(){
		if(outputStream==null){
			outputStream=new UDTOutputStream(this);
		}
		return outputStream;
	}
	
	public final UDTSession getSession(){
		return session;
	}
	
	/**
	 * write single block of data without waiting for any acknowledgement
	 * @param data
	 */
	protected void doWrite(byte[]data)throws IOException{
		doWrite(data, 0, data.length);
		
	}
	
	/**
	 * write the given data 
	 * @param data - the data array
	 * @param offset - the offset into the array
	 * @param length - the number of bytes to write
	 * @throws IOException
	 */
	protected void doWrite(byte[]data, int offset, int length)throws IOException{
		try{
			doWrite(data, offset, length, Integer.MAX_VALUE, TimeUnit.MILLISECONDS);
		}catch(InterruptedException ie){
			IOException io=new IOException();
			io.initCause(ie);
			throw io;
		}
	}
	
	/**
	 * write the given data, waiting at most for the specified time if the queue is full
	 * @param data
	 * @param offset
	 * @param length
	 * @param timeout
	 * @param units
	 * @throws IOException - if data cannot be sent
	 * @throws InterruptedException
	 */
	protected void doWrite(byte[]data, int offset, int length, int timeout, TimeUnit units)throws IOException,InterruptedException{
		int chunksize=session.getDatagramSize()-24;//need some bytes for the header
		ByteBuffer bb=ByteBuffer.wrap(data,offset,length);
		long seqNo=0;
		while(bb.remaining()>0){
			int len=Math.min(bb.remaining(),chunksize);
			byte[]chunk=new byte[len];
			bb.get(chunk);
			DataPacket packet=new DataPacket();
			seqNo=sender.getNextSequenceNumber();
			packet.setPacketSequenceNumber(seqNo);
			packet.setSession(session);
			packet.setDestinationID(session.getDestination().getSocketID());
			packet.setData(chunk);
			//put the packet into the send queue
			if(!sender.sendUdtPacket(packet, timeout, units)){
				throw new IOException("Queue full");
			}
		}
		if(length>0)active=true;
	}
	/**
	 * will block until the outstanding packets have really been sent out
	 * and acknowledged
	 */
	protected void flush() throws InterruptedException{
		if(!active)return;
		final long seqNo=sender.getCurrentSequenceNumber();
		if(seqNo<0)throw new IllegalStateException();
		while(!sender.isSentOut(seqNo)){
			Thread.sleep(5);
		}
		if(seqNo>-1){
			//wait until data has been sent out and acknowledged
			while(active && !sender.haveAcknowledgementFor(seqNo)){
				sender.waitForAck(seqNo);
			}
		}
		//TODO need to check if we can pause the sender...
		//sender.pause();
	}
	
	//writes and wait for ack
	protected void doWriteBlocking(byte[]data)throws IOException, InterruptedException{
		doWrite(data);
		flush();
	}
	
	/**
	 * close the connection
	 * @throws IOException
	 */
	public void close()throws IOException{
		if(inputStream!=null)inputStream.close();
		if(outputStream!=null)outputStream.close();
		active=false;
	}

}
