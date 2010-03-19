package udt;
import junit.framework.TestCase;
import udt.receiver.AckHistoryEntry;
import udt.receiver.AckHistoryWindow;
import udt.receiver.PacketHistoryWindow;
import udt.receiver.PacketPairWindow;
import udt.sender.SenderLossList;
import udt.sender.SenderLossListEntry;
import udt.util.CircularArray;
import udt.util.FlowWindow;

/*
 * tests for the various list and queue classes
 */
public class TestList extends TestCase{

	public void testCircularArray(){
		CircularArray<Integer>c=new CircularArray<Integer>(5);
		for(int i=0;i<5;i++)c.add(i);
		assertEquals(5,c.size());
		c.add(6);
		assertEquals(5,c.size());
		System.out.println(c);
		c.add(7);
		System.out.println(c);
		for(int i=8;i<11;i++)c.add(i);
		System.out.println(c);
		c.add(11);
		System.out.println(c);
	}
	
	public void testFlowWindow(){
		FlowWindow<Long>f=new FlowWindow<Long>(5);
		for(int i=0;i<5;i++){
			System.out.println(i);
			assertTrue(f.add(Long.valueOf(i)));
		}
		assertFalse(f.add(0l));
		f.setCapacity(6);
		assertTrue(f.add(0l));
	}
	
	public void testPacketHistoryWindow(){

		PacketHistoryWindow packetHistoryWindow = new PacketHistoryWindow(16);
		
		for(int i=0;i<17;i++){
			packetHistoryWindow.add(i*5000l);
		}
		//packets arrive every 5 ms, so packet arrival rate is 200/sec
		assertEquals(200.0,packetHistoryWindow.getPacketArrivalSpeed());
	}
	

	public void testPacketPairWindow(){
		long[]values={2,4,6};
		PacketPairWindow p=new PacketPairWindow(16);
		for(int i=0;i<values.length;i++){
			p.add(values[i]);
		}
		//assertEquals(10.0d, p.computeMedianTimeInterval());
		
		System.out.println(p.toString());
		System.out.println("MedianTimeInterval: "+p.computeMedianTimeInterval());
		
		System.out.println(p.toString());
		System.out.println("MedianTimeInterval: "+p.computeMedianTimeInterval());
		
		//assertEquals(10.0d, p.);
		
		//long[] arrivaltimes = {12, 12, 12, 12};
		//PacketPairWindow p1=new PacketPairWindow(16);
		//for(int i=0;i<values.length;i++){
		//	p1.insert(arrivaltimes[i]);
		//}
		//assertEquals(12.0d, p1.computeMedianTimeInterval());
		
	}
	
	
	

	public void testAckHistoryWindow(){
		AckHistoryEntry ackSeqNrA = new AckHistoryEntry( 0,1,1263465050);
		
		AckHistoryEntry ackSeqNrB = new AckHistoryEntry(1,2,1263465054);
		
		AckHistoryEntry ackSeqNrC = new AckHistoryEntry(2,3,1263465058);
		
		AckHistoryWindow recvWindow = new AckHistoryWindow(3);
		recvWindow.add(ackSeqNrA);
		recvWindow.add(ackSeqNrB);
		recvWindow.add(ackSeqNrC);
		AckHistoryEntry entryA = recvWindow.getEntry(1);
		long storageTimeA = entryA.getSentTime();
		long storageTimeA_ =recvWindow.getTime(1);
		System.out.println("storageTimeA bzw A_ "+storageTimeA+"  "+storageTimeA_);
		
	}

	public void testSenderLossList1(){
		SenderLossListEntry A = new SenderLossListEntry(7);
		SenderLossListEntry B = new SenderLossListEntry(8);
		SenderLossListEntry C = new SenderLossListEntry(1);
		SenderLossList l=new SenderLossList();
		l.insert(A);
		l.insert(B);
		l.insert(C);
		SenderLossListEntry oldest=l.getFirstEntry();
		assertEquals(C,oldest);
	}

}
