package udt.receiver;

import udt.util.Util;

/**
 * an entry in the {@link ReceiverLossList}
 */
public class ReceiverLossListEntry implements Comparable<ReceiverLossListEntry> {

	private final long sequenceNumber ;
	private	long lastFeedbacktime;
	private long k = 2;

	/**
	 * constructor
	 * @param sequenceNumber
	 */
	public ReceiverLossListEntry(long sequenceNumber){
		if(sequenceNumber<=0)throw new IllegalArgumentException();
		this.sequenceNumber = sequenceNumber;	
		this.lastFeedbacktime=Util.getCurrentTime();
	}


	/**
	 * call once when this seqNo is fed back in NAK
	 */
	public void feedback(){
		k++;
		lastFeedbacktime=Util.getCurrentTime();
	}

	public long getSequenceNumber() {
		return sequenceNumber;
	}

	/**
	 * k is initialised as 2 and increased by 1 each time the number is fed back
	 * @return k the number of times that this seqNo has been feedback in NAK
	 */
	public long getK() {
		return k;
	}

	public long getLastFeedbackTime() {
		return lastFeedbacktime;
	}
	
	/**
	 * order by increasing sequence number
	 */
	public int compareTo(ReceiverLossListEntry o) {
		return (int)(sequenceNumber-o.sequenceNumber);
	}


	public String toString(){
		return sequenceNumber+"[k="+k+",time="+lastFeedbacktime+"]";
	}

}
