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

package udt.packets;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import udt.UDTPacket;
import udt.UDTSession;

public class DataPacket implements UDTPacket, Comparable<UDTPacket>{

	private byte[] data ;
	private long packetSequenceNumber;
	private long messageNumber;
	private long timeStamp;
	private long destinationID;

	private UDTSession session;
	
	public DataPacket(){
	}

	/**
	 * create a DataPacket
	 * @param encodedData - network data
	 */
	public DataPacket(byte[] encodedData){
		this(encodedData,encodedData.length);
	}

	public DataPacket(byte[] encodedData, int length){
		decode(encodedData,length);
	}
	
	void decode(byte[]encodedData,int length){
		packetSequenceNumber=PacketUtil.decode(encodedData, 0);
		messageNumber=PacketUtil.decode(encodedData, 4);
		timeStamp=PacketUtil.decode(encodedData, 8);
		destinationID=PacketUtil.decode(encodedData, 12);
		data=new byte[length-16];
		System.arraycopy(encodedData, 16, data, 0, data.length);
	}


	public byte[] getData() {
		return this.data;
	}

	public double getLength(){
		return data.length;
	}

	/*
	 * aplivation data
	 * @param
	 */

	public void setData(byte[] data) {
		this.data = data;
	}

	public long getPacketSequenceNumber() {
		return this.packetSequenceNumber;
	}

	public void setPacketSequenceNumber(long sequenceNumber) {
		this.packetSequenceNumber = sequenceNumber;
	}


	public long getMessageNumber() {
		return this.messageNumber;
	}

	public void setMessageNumber(long messageNumber) {
		this.messageNumber = messageNumber;
	}

	public long getDestinationID() {
		return this.destinationID;
	}

	public long getTimeStamp() {
		return this.timeStamp;
	}



	public void setDestinationID(long destinationID) {
		this.destinationID=destinationID;
	}

	public void setTimeStamp(long timeStamp) {
		this.timeStamp=timeStamp;
	}



	/**
	 * return the header according to specification p.5
	 * @return
	 */
	//TODO order?
	public byte[] getHeader(){
		//sequence number with highest bit set to "0"
		try{
			ByteArrayOutputStream bos=new ByteArrayOutputStream(16);
			bos.write(PacketUtil.encodeSetHighest(false, packetSequenceNumber));
			bos.write(PacketUtil.encode(messageNumber));
			bos.write(PacketUtil.encode(timeStamp));
			bos.write(PacketUtil.encode(destinationID));
			return bos.toByteArray();

		}catch(IOException ioe){/*can't happen*/
			return null;
		}
	}

	/**
	 * complete header+data packet for transmission
	 */
	public byte[] getEncoded(){
		byte[] header=getHeader();
		//header.length is 16
		byte[] result=new byte[16+data.length];
		System.arraycopy(header, 0, result, 0, 16);
		System.arraycopy(data, 0, result, 16, data.length);
		return result;
	}

	public boolean isControlPacket(){
		return false;
	}

	public boolean forSender(){
		return false;
	}

	public boolean isConnectionHandshake(){
		return false;
	}
	
	public int getControlPacketType(){
		return -1;
	}
	
	public UDTSession getSession() {
		return session;
	}

	public void setSession(UDTSession session) {
		this.session = session;
	}

	public int compareTo(UDTPacket other){
		return (int)(getPacketSequenceNumber()-other.getPacketSequenceNumber());
	}
}
