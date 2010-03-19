package udt.packets;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import udt.UDTPacket;

public abstract class ControlPacket implements UDTPacket{
	
	protected int contrlPktTyp;

	//used for ACK and ACK2
	protected long ackSequenceNumber;
	
	protected long messageNumber;
	
	protected long timeStamp;
	
	protected long destinationID;
	
	protected byte[] controlInformation;
    
	public ControlPacket(){
    	
    }
    
	
	public int getControlPaketType() {
		return contrlPktTyp;
	}


	public void setControlPaketType(int packetTyp) {
		this.contrlPktTyp = packetTyp;
	}


	public long getAckSequenceNumber() {
		return ackSequenceNumber;
	}
	 public void setAckSequenceNumber(long ackSequenceNumber) {
		this.ackSequenceNumber = ackSequenceNumber;
	}


	public long getMessageNumber() {
		return messageNumber;
	}
	public void setMessageNumber(long messageNumber) {
		this.messageNumber = messageNumber;
	}
	
	
	public long getTimeStamp() {
		return timeStamp;
	}
	
	public void setTimeStamp(long timeStamp) {
		this.timeStamp = timeStamp;
	}
	
	
	public long getDestinationID() {
		return destinationID;
	}
	public void setDestinationID(long destinationID) {
		this.destinationID = destinationID;
	}
	
	
	/**
	 * return the header according to specification p.5
	 * @return
	 */
	//TODO order?!?!?
	public byte[] getHeader(){
//		//sequence number with highest bit set to "0"
		try{
			ByteArrayOutputStream bos=new ByteArrayOutputStream(16);
			bos.write(PacketUtil.encodeHighesBitTypeAndSeqNumber(true, contrlPktTyp, ackSequenceNumber));
			bos.write(PacketUtil.encode(messageNumber));
			bos.write(PacketUtil.encode(timeStamp));
			bos.write(PacketUtil.encode(destinationID));
			return bos.toByteArray();
		}catch(IOException ioe){/*can't happen*/
			return null;
		}
	}
	
	/**
	 * this method builds the control information
	 * from the control parameters
	 * @return
	 */
	public abstract byte[] encodeControlInformation(); 

	/**
	 * complete header+ControlInformation packet for transmission
	 */
	
	public byte[] getEncoded(){
		byte[] header=getHeader();
		byte[] controlInfo=encodeControlInformation();
		byte[] result=controlInfo!=null?
				new byte[header.length + controlInfo.length]:
				new byte[header.length]; 
		System.arraycopy(header, 0, result, 0, header.length);
		if(controlInfo!=null){
			System.arraycopy(controlInfo, 0, result, header.length, controlInfo.length);
		}
		return result;
		
	};

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ControlPacket other = (ControlPacket) obj;
		if (ackSequenceNumber != other.ackSequenceNumber)
			return false;
		if (contrlPktTyp != other.contrlPktTyp)
			return false;
		//if (!Arrays.equals(controlInformation, other.controlInformation))
		//	return false;
		if (destinationID != other.destinationID)
			return false;
		if (messageNumber != other.messageNumber)
			return false;
		if (timeStamp != other.timeStamp)
			return false;
		return true;
	}

	public boolean isControlPacket(){
		return true;
	}
	
	public boolean forSender(){
		return true;
	}
	
	public static enum ControlPacketType {
		
		CONNECTION_HANDSHAKE,
		KEEP_ALIVE,
		ACK,
		NAK,
		UNUNSED_1,
		SHUTDOWN,
		ACK2,
		MESSAGE_DROP_REQUEST,
		UNUNSED_2,
		UNUNSED_3,
		UNUNSED_4,
		UNUNSED_5,
		UNUNSED_6,
		UNUNSED_7,
		UNUNSED_8,
		USER_DEFINED,
		
	}
	
}
