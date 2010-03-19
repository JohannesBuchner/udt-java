package udt.sender;

import udt.util.Util;

/**
 * the sender loss list stores information about lost data packets on the
 * sender side, ordered by increasing sequence number
 * @see SenderLossListEntry
 */
public class SenderLossListEntry implements Comparable<SenderLossListEntry>{
	
	private final long sequenceNumber ;
	
	//time when the loss list entry was created
	private final long storageTime;
	
	public SenderLossListEntry(long sequenceNumber){
		if(sequenceNumber<0)throw new IllegalArgumentException();
		this.sequenceNumber = sequenceNumber;	
		storageTime=Util.getCurrentTime();
	}

	public long getSequenceNumber() {
		return sequenceNumber;
	}

	/**
	 * get the age of this loss sequence number
	 * @return
	 */
	public long getAge() {
		return Util.getCurrentTime()-storageTime;
	}

	/**
	 * used to order entries by increasing sequence number
	 */
	public int compareTo(SenderLossListEntry o) {
		return (int)(sequenceNumber-o.sequenceNumber);
	}

}
