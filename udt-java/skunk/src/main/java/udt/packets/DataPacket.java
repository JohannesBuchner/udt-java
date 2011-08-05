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

import udt.util.ObjectPool;
import udt.util.Recycler.Recyclable;
import udt.UDTPacket;
import udt.UDTSession;

public class DataPacket implements UDTPacket, Comparable<UDTPacket>, Recyclable{
    private static final byte[] emptydata = new byte[0];
    private static ObjectPool<DataPacket> packetPool = null;
    /**
     * Static method which can be used to tune the size of the object
     * pool for packet recycling.  Must be called prior to all static factory
     * methods and static discard method.  Default pool size is 40.
     * 
     * @param size
     */
    private static void initializeObjectPool(int size) {
        if (packetPool != null) return;
        packetPool = new ObjectPool<DataPacket>(size);

    }
    
    public static DataPacket create(byte[] data, int packetSequenceNumber, 
            int messageNumber, int timeStamp, UDTSession session){
        initializeObjectPool(40);
        DataPacket p = packetPool.get();
        if ( p == null ) {
            p = new DataPacket(data, packetSequenceNumber, messageNumber, 
                timeStamp, session);
        } else {
            p.encode(data, packetSequenceNumber, messageNumber, timeStamp, session);
        }       
        return p;
    }
    
    public static DataPacket create(byte[] encodedData){
        initializeObjectPool(40);
        DataPacket p = packetPool.get();
        if ( p == null ) {
            p = new DataPacket(encodedData);
        } else {
            p.decode(encodedData, encodedData.length);
        }       
        return p;
    }
    
    public static DataPacket create(byte[] encodedData, int length){
        initializeObjectPool(40);
        DataPacket p = packetPool.get();
        if ( p == null ) {
            p = new DataPacket(encodedData, length);
        } else {
            p.decode(encodedData, length);
        }       
        return p;
    }
    
    
    public static void discard(DataPacket p){
        initializeObjectPool(40);
        packetPool.accept(p);
    }

	private byte[] data ;
	private long packetSequenceNumber;
	private long messageNumber;
	private long timeStamp;
	private long destinationSocketID;

	private UDTSession session;

	private int dataLength;
	
	public DataPacket(){
	}

        DataPacket(byte[] data, int packetSequenceNumber, int messageNumber,
                int timeStamp, UDTSession session){
            encode(data, packetSequenceNumber, messageNumber, timeStamp, session);
        }
        

	/**
	 * create a DataPacket from the given raw data
	 * 
	 * @param encodedData - network data
	 */
	public DataPacket(byte[] encodedData){
		this(encodedData,encodedData.length);
	}

	public DataPacket(byte[] encodedData, int length){
		decode(encodedData,length);
	}
	
	private void decode(byte[]encodedData,int length){
		packetSequenceNumber=PacketUtil.decode(encodedData, 0);
		messageNumber=PacketUtil.decode(encodedData, 4);
		timeStamp=PacketUtil.decode(encodedData, 8);
		destinationSocketID=PacketUtil.decode(encodedData, 12);
                dataLength=length-16;
		data=new byte[dataLength];
		System.arraycopy(encodedData, 16, data, 0, dataLength);
	}

        private void encode(byte[] data, long packetSequenceNumber, long messageNumber,
                long timeStamp, UDTSession session){
            this.data=data;
            this.dataLength=data.length;
            this.packetSequenceNumber=packetSequenceNumber;
            this.messageNumber=messageNumber;
            this.timeStamp=timeStamp;
            this.destinationSocketID= (session == null) ? 0L : session.getSocketID();
            this.session=session;
        }

        
        
	public byte[] getData() {
		return this.data;
	}

	public double getLength(){
		return dataLength;
	}

	public void setLength(int length){
		dataLength=length;
	}
	
	public void setData(byte[] data) {
		this.data = data;
		dataLength=data.length;
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
		return this.destinationSocketID;
	}

	public long getTimeStamp() {
		return this.timeStamp;
	}

	public void setDestinationID(long destinationID) {
		this.destinationSocketID=destinationID;
	}

	public void setTimeStamp(long timeStamp) {
		this.timeStamp=timeStamp;
	}

	/**
	 * complete header+data packet for transmission
	 */
	public byte[] getEncoded(){
		//header.length is 16
		byte[] result=new byte[16+dataLength];
		System.arraycopy(PacketUtil.encode(packetSequenceNumber), 0, result, 0, 4);
		System.arraycopy(PacketUtil.encode(messageNumber), 0, result, 4, 4);
		System.arraycopy(PacketUtil.encode(timeStamp), 0, result, 8, 4);
		System.arraycopy(PacketUtil.encode(destinationSocketID), 0, result, 12, 4);
		System.arraycopy(data, 0, result, 16, dataLength);
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

    public void recycle() {
            this.data=emptydata;
            this.dataLength=0;
            this.packetSequenceNumber=0;
            this.messageNumber=0;
            this.timeStamp=0;
            this.destinationSocketID=0;
            this.session=null;
}
}
