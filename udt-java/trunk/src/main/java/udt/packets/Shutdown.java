
package udt.packets;


public class Shutdown extends ControlPacket{
	
	public Shutdown(){
		this.contrlPktTyp=ControlPacketType.SHUTDOWN.ordinal();	
	}
	
	@Override
	public byte[] encodeControlInformation(){
		return null;
	}
	
	@Override
	public boolean forSender(){
		return false;
	}

}

