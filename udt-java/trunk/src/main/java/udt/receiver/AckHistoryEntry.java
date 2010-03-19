package udt.receiver;

import udt.util.Util;

/**
 * store the Sent Acknowledge packet number
 * and the time it is sent out
 * 
 *
 */
public class AckHistoryEntry {
	
	private final long ackSequenceNumber;
	//the sequence number prior to which all the packets have been received
	private final long ackNumber;
	//time when the Acknowledgement entry was sent
	private final long  sentTime;
	
	public AckHistoryEntry(long ackSequenceNumber, long ackNumber, long sentTime){
		this.ackSequenceNumber= ackSequenceNumber;
		this.ackNumber = ackNumber;
		this.sentTime = sentTime;
	}
	

	public long getAckSequenceNumber() {
		return ackSequenceNumber;
	}

	public long getAckNumber() {
		return ackNumber;
	}

	public long getSentTime() {
		return sentTime;
	}
	
	/**
	 * get the age of this sent ack sequence number
	 * @return
	 */
	public long getAge() {
		return Util.getCurrentTime()-sentTime;
	}



	
}
