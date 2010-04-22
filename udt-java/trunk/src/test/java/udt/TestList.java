package udt;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;

import junit.framework.TestCase;
import udt.packets.DataPacket;
import udt.packets.KeepAlive;
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
		assertEquals(4.0d, p.computeMedianTimeInterval());
		
		long[] arrivaltimes = {12, 12, 12, 12};
		PacketPairWindow p1=new PacketPairWindow(16);
		for(int i=0;i<values.length;i++){
			p1.add(arrivaltimes[i]);
		}
		assertEquals(12.0d, p1.computeMedianTimeInterval());
		
	}
	
	
	

	public void testAckHistoryWindow(){
		AckHistoryEntry ackSeqNrA = new AckHistoryEntry(0,1,1263465050);
		AckHistoryEntry ackSeqNrB = new AckHistoryEntry(1,2,1263465054);
		AckHistoryEntry ackSeqNrC = new AckHistoryEntry(2,3,1263465058);
		
		AckHistoryWindow recvWindow = new AckHistoryWindow(3);
		recvWindow.add(ackSeqNrA);
		recvWindow.add(ackSeqNrB);
		recvWindow.add(ackSeqNrC);
		AckHistoryEntry entryA = recvWindow.getEntry(1);
		assertEquals(1263465050, entryA.getSentTime());
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

	public void testReceiverInputQueue(){
		BlockingQueue<UDTPacket> q=new PriorityBlockingQueue<UDTPacket>(5);
		UDTPacket control = new KeepAlive();
		DataPacket d1=new DataPacket();
		d1.setPacketSequenceNumber(1);
		DataPacket d2=new DataPacket();
		d2.setPacketSequenceNumber(2);
		q.offer(d2);
		q.offer(d1);
		q.offer(control);
		
		UDTPacket p1=q.poll();
		assertTrue(p1.isControlPacket());
		
		UDTPacket p2=q.poll();
		assertFalse(p2.isControlPacket());
		//check ordering by sequence number
		assertEquals(1,p2.getPacketSequenceNumber());
		
		UDTPacket p3=q.poll();
		assertFalse(p3.isControlPacket());
		assertEquals(2,p3.getPacketSequenceNumber());
		
		
	}
	
}
