package udt.packets;

import udt.packets.ControlPacket.ControlPacketType;

public interface ControlInformation {

	byte[] getEncodedControlInformation();
	
	ControlPacketType getType();
}
