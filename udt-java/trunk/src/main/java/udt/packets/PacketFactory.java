package udt.packets;
import udt.UDTPacket;
import udt.packets.ControlPacket.*;

public class PacketFactory {

	/**
	 * creates a Control or Data packet depending on the highest bit
	 * of the first 32 bit of data
	 * @param packetData
	 * @return
	 */
	public static UDTPacket createPacket(byte[]encodedData){
		boolean isControl=(encodedData[0]&128) !=0 ;
		if(isControl)return createControlPacket(encodedData,encodedData.length);
		return new DataPacket(encodedData);
	}
	
	public static UDTPacket createPacket(byte[]encodedData,int length){
		boolean isControl=(encodedData[0]&128) !=0 ;
		if(isControl)return createControlPacket(encodedData,length);
		return new DataPacket(encodedData,length);
	}
	
	/**
	 * create the right type of control packet based on the packet data 
	 * @param packetData
	 * @return
	 */
	public static ControlPacket createControlPacket(byte[]encodedData,int length){
	
		ControlPacket packet=null;
		
		int pktType=PacketUtil.decodeType(encodedData, 0);
		long  ackSeqNo =PacketUtil.decodeAckSeqNr(encodedData, 0); 
		long  msgNr = PacketUtil.decode(encodedData, 4);
		long  timeStamp = PacketUtil.decode(encodedData,8) ;
		long  destID = PacketUtil.decode(encodedData,12);
		byte[] controlInformation = new byte[length-16];
		System.arraycopy(encodedData,16,controlInformation,0,controlInformation.length);
			
		//TYPE 0000:0
		if(ControlPacketType.CONNECTION_HANDSHAKE.ordinal()==pktType){
			packet=new ConnectionHandshake(controlInformation);
		}
		//TYPE 0001:1
		if(ControlPacketType.KEEP_ALIVE.ordinal()==pktType){
			packet=new KeepAlive();
		}
		//TYPE 0010:2
		if(ControlPacketType.ACK.ordinal()==pktType){
			packet=new Acknowledgement(controlInformation);
		}
		//TYPE 0011:3
		if(ControlPacketType.NAK.ordinal()==pktType){
			packet=new NegativeAcknowledgement(controlInformation);
		}
		//TYPE 0101:5
		if(ControlPacketType.SHUTDOWN.ordinal()==pktType){
			packet=new Shutdown();
		}
		//TYPE 0110:6
		if(ControlPacketType.ACK2.ordinal()==pktType){
			packet=new Acknowledgment2(controlInformation);
		}
		//TYPE 0111:7
		if(ControlPacketType.MESSAGE_DROP_REQUEST.ordinal()==pktType){
			packet=new MessageDropRequest(controlInformation);
		}
		//TYPE 1111:8
		if(ControlPacketType.USER_DEFINED.ordinal()==pktType){
			packet=new UserDefined(controlInformation);
		}
		
		if(packet!=null){
			packet.setControlPaketType(pktType);
			packet.setAckSequenceNumber(ackSeqNo);
			packet.setMessageNumber(msgNr);
			packet.setTimeStamp(timeStamp);
			packet.setDestinationID(destID);
		}
		return packet;
		
	}
	
}
