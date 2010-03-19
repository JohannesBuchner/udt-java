package udt.packets;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import udt.UDTPacket;

public class DataPacket implements UDTPacket, Comparable<DataPacket>{

	private byte[] data ;
	private long packetSequenceNumber;
	private long messageNumber;
	private long timeStamp;
	private long destinationID;

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
		packetSequenceNumber =PacketUtil.decode(encodedData, 0);
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
		byte[] result=new byte[header.length+data.length];
		System.arraycopy(header, 0, result, 0, header.length);
		System.arraycopy(data, 0, result, header.length, data.length);
		return result;
	}

	public boolean isControlPacket(){
		return false;
	}

	public boolean forSender(){
		return false;
	}

	//Compare data packets by their sequence number
	public int compareTo(DataPacket other){
		return (int)(other.packetSequenceNumber-packetSequenceNumber);
	}

}
