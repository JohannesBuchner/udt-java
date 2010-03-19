package udt.packets;


public class KeepAlive extends ControlPacket{
	
	public KeepAlive(){
		this.contrlPktTyp=ControlPacketType.KEEP_ALIVE.ordinal();	
	}
	
	@Override
	public byte[] encodeControlInformation(){
		return null;
	}
}
