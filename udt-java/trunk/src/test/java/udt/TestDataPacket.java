package udt;

import junit.framework.TestCase;
import udt.packets.DataPacket;

public class TestDataPacket extends TestCase {

	public void testSequenceNumber1(){
		DataPacket p=new DataPacket();
		p.setPacketSequenceNumber(1);
		byte[]x=p.getHeader();
		byte highest=x[0];
		//check highest bit is "0" for DataPacket
		assertEquals(0, highest & 128);
		byte lowest=x[3];
		assertEquals(1, lowest);
	}

	public void testEncoded(){
		DataPacket p=new DataPacket();
		p.setPacketSequenceNumber(1);
		byte[] data="test".getBytes();
		p.setData(data);
		byte[]encoded=p.getEncoded();
		int headerLength=p.getHeader().length;
		assertEquals(data.length+headerLength,encoded.length);
		byte[]encData=new byte[data.length];
		System.arraycopy(encoded, headerLength, encData, 0, data.length);
		String s=new String(encData);
		assertEquals("test", s);
		System.out.println("String s = " + s);
	}


	public void testDecode1(){

		DataPacket testPacket1=new DataPacket();
		testPacket1.setPacketSequenceNumber(127);
		testPacket1.setDestinationID(1);
		byte[] data1="Hallo".getBytes();
		testPacket1.setData(data1);

		//get the encoded data
		byte[]encodedData=testPacket1.getEncoded();

		int headerLength=testPacket1.getHeader().length;
		assertEquals(data1.length+headerLength,encodedData.length);

		byte[]payload=new byte[data1.length];
		System.arraycopy(encodedData, headerLength, payload, 0, data1.length);
		String s1=new String(payload);
		assertEquals("Hallo", s1);

		System.out.println("String s1 = " + s1);
		System.out.println("tesPacket1Length = "+ testPacket1.getLength());
		System.out.println("sequenceNumber1 = " + testPacket1.getPacketSequenceNumber());
		System.out.println("messageNumber 1= " + testPacket1.getMessageNumber());
		System.out.println("timeStamp1 = " + testPacket1.getTimeStamp());
		System.out.println("destinationID1 = " + testPacket1.getDestinationID());
		System.out.println("data1 = " + new String(testPacket1.getData()));


		//create a new DataPacket from the encoded data
		DataPacket testPacket2=new DataPacket(encodedData);
		// and test
		System.out.println("tesPacket2Length = "+ testPacket2.getLength());
		System.out.println("sequenceNumber2 = " + testPacket2.getPacketSequenceNumber());
		System.out.println("messageNumber2 = " + testPacket2.getMessageNumber());
		System.out.println("timeStamp2 = " + testPacket2.getTimeStamp());
		System.out.println("destinationID1 = " + testPacket1.getDestinationID());
		System.out.println("data2 = " + new String(testPacket2.getData()));

		assertEquals(127,testPacket2.getPacketSequenceNumber());


	}

	public void testEncodeDecode1(){
		DataPacket dp=new DataPacket();
		dp.setPacketSequenceNumber(127);
		dp.setMessageNumber(268435457);
		dp.setTimeStamp(128);
		dp.setDestinationID(255);
		dp.setData("test".getBytes());

		byte[]encodedData1=dp.getEncoded();

		DataPacket dp2=new DataPacket(encodedData1);
		assertEquals(127,dp2.getPacketSequenceNumber());
		assertEquals(268435457,dp2.getMessageNumber());
		assertEquals(128,dp2.getTimeStamp());
		assertEquals(255,dp2.getDestinationID());
		assertEquals("test", new String(dp2.getData()));
	}


}
