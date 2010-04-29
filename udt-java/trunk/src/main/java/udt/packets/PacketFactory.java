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
		else if(ControlPacketType.KEEP_ALIVE.ordinal()==pktType){
			packet=new KeepAlive();
		}
		//TYPE 0010:2
		else if(ControlPacketType.ACK.ordinal()==pktType){
			packet=new Acknowledgement(controlInformation);
		}
		//TYPE 0011:3
		else if(ControlPacketType.NAK.ordinal()==pktType){
			packet=new NegativeAcknowledgement(controlInformation);
		}
		//TYPE 0101:5
		else if(ControlPacketType.SHUTDOWN.ordinal()==pktType){
			packet=new Shutdown();
		}
		//TYPE 0110:6
		else if(ControlPacketType.ACK2.ordinal()==pktType){
			packet=new Acknowledgment2(controlInformation);
		}
		//TYPE 0111:7
		else if(ControlPacketType.MESSAGE_DROP_REQUEST.ordinal()==pktType){
			packet=new MessageDropRequest(controlInformation);
		}
		//TYPE 1111:8
		else if(ControlPacketType.USER_DEFINED.ordinal()==pktType){
			packet=new UserDefined(controlInformation);
		}
		
		if(packet!=null){
			packet.setAckSequenceNumber(ackSeqNo);
			packet.setMessageNumber(msgNr);
			packet.setTimeStamp(timeStamp);
			packet.setDestinationID(destID);
		}
		return packet;
		
	}
	
}
