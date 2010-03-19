package udt.packets;

import udt.UDTSender;

/**
 * Ack2 is sent by the {@link UDTSender} as immediate reply to an {@link Acknowledgement}
 */
public class Acknowledgment2 extends ControlPacket{

		public Acknowledgment2(){
			this.contrlPktTyp=ControlPacketType.ACK2.ordinal();	
		}
		
		public Acknowledgment2(byte[]controlInformation){
			this();
			decode(controlInformation );
		}
		
		void decode(byte[]data){
		}

		public boolean forSender(){
			return false;
		}
		
		private static final byte[]empty=new byte[0];
		@Override
		public byte[] encodeControlInformation(){
			return empty;
		}
	}



