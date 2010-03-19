package udt.sender;

/**
 * stores a data packet sequence number and the time it was sent 
 */
public class SenderListEntry implements Comparable<SenderListEntry>{
	
	private final long sequenceNumber;
	//departure time
	private final long sentTime;
	
	public SenderListEntry(long sequenceNumber, long sentTime){
		this.sequenceNumber= sequenceNumber;
		this.sentTime= sentTime;
	}

	public long getSequenceNumber() {
		return sequenceNumber;
	}

	public long getSentTime() {
		return sentTime;
	}

	public String toString(){
		return sequenceNumber+"[time="+sentTime+"]";
	}

	
	//used to order the entries in order of increasing sequence numbers 
	public int compareTo(SenderListEntry o) {
		return (int)(sequenceNumber-o.sequenceNumber);
	}

	
	
}
