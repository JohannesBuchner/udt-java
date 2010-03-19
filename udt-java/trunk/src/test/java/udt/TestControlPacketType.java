package udt;

import udt.packets.ControlPacket;
import udt.packets.ControlPacket.ControlPacketType;
import junit.framework.TestCase;

public class TestControlPacketType extends TestCase {

	public void testSequenceNumber1(){
		ControlPacket p=new DummyControlPacket();
		p.setAckSequenceNumber(1);
		byte[]x=p.getHeader();
		byte highest=x[0];
		//check highest bit is "1" for ControlPacket
		
		assertEquals(128, highest & 0x80);
		byte lowest=x[3];
		assertEquals(1, lowest);
	}
	
	public void testControlPacketTypes(){
		ControlPacketType t=ControlPacketType.CONNECTION_HANDSHAKE;
		assertEquals(0,t.ordinal());
		t=ControlPacketType.KEEP_ALIVE;
		assertEquals(1,t.ordinal());
		t=ControlPacketType.ACK;
		assertEquals(2,t.ordinal());
		t=ControlPacketType.NAK;
		assertEquals(3,t.ordinal());
		t=ControlPacketType.SHUTDOWN;
		assertEquals(5,t.ordinal());
		t=ControlPacketType.ACK2;
		assertEquals(6,t.ordinal());
		t=ControlPacketType.MESSAGE_DROP_REQUEST;
		assertEquals(7,t.ordinal());
		t=ControlPacketType.USER_DEFINED;
		assertEquals(15,t.ordinal());
	}
}
