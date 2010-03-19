package udt.packets;


public class UserDefined extends ControlPacket{
	
	//Explained by bits 4-15,
	//reserved for user defined Control Packet
	public UserDefined(byte[]controlInformation){
		this.controlInformation=controlInformation;
	}

	@Override
	public byte[] encodeControlInformation() {
		return null;
	}
}
