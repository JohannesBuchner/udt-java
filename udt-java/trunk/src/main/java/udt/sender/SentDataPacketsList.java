package udt.sender;
import java.util.Iterator;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * stores the sequence numbers of the data packets sent out, and the time of sending.
 * Entries are ordered in order of increasing sequence number
 * @see SenderListEntry
 */
public class SentDataPacketsList {

	private final PriorityBlockingQueue<SenderListEntry>listOfSentPacketSeqNo;

	public SentDataPacketsList(){
		listOfSentPacketSeqNo = new PriorityBlockingQueue<SenderListEntry>(64);
	} 

	public void insert(SenderListEntry obj){
		listOfSentPacketSeqNo.add(obj);
	}

	public void remove(long seqNo){
		Iterator<SenderListEntry>iterator=listOfSentPacketSeqNo.iterator();
		while(iterator.hasNext()){
			SenderListEntry e=iterator.next();
			if(e.getSequenceNumber()==seqNo){
				iterator.remove();
				return;
			}
		}
	}

	public boolean isEmpty(){
		return listOfSentPacketSeqNo.isEmpty();
	}

}
