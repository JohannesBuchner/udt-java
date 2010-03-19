package udt.sender;
import java.util.Iterator;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * stores the sequence number of the lost packets in increasing order
 */
public class SenderLossList {

	private final PriorityBlockingQueue<SenderLossListEntry>backingList;
	
	/**
	 * create a new sender lost list
	 */
	public SenderLossList(){
		backingList = new PriorityBlockingQueue<SenderLossListEntry>(16);
	}

	public void insert(SenderLossListEntry obj){
		backingList.add(obj);
	}

	public void remove(long seqNo){
		Iterator<SenderLossListEntry>iterator=backingList.iterator();
		while(iterator.hasNext()){
			SenderLossListEntry e=iterator.next();
			if(e.getSequenceNumber()==seqNo){
				iterator.remove();
				return;
			}
		}
	}

	/**
	 * gets the loss list entry with the lowest sequence number
	 */
	public SenderLossListEntry getFirstEntry(){
		return backingList.poll();
	}

	public boolean isEmpty(){
		return backingList.isEmpty();
	}

}
