package udt.util;

import java.util.Random;


/**
 * Handle sequence numbers, taking the range of 0 - (2^31 - 1) into account<br/>
 */

public class SequenceNumber {

	private final static int maxOffset=0x3FFFFFFF;

	private final static long maxSequenceNo=0x7FFFFFFF;


	/**
	 * compare seq1 and seq2. Returns zero, if they are equal, a negative value if seq1 is smaller than
	 * seq2, and a positive value if seq1 is larger than seq2.
	 * 
	 * @param seq1
	 * @param seq2
	 */
    public static long compare(long seq1, long seq2){
    	return (Math.abs(seq1 - seq2) < maxOffset) ? (seq1 - seq2) : (seq2 - seq1);
    }

    /**
     * length from the first to the second sequence number, including both
     */
    public static long length(long seq1, long seq2)
    {return (seq1 <= seq2) ? (seq2 - seq1 + 1) : (seq2 - seq1 + maxSequenceNo + 2);}

    
	/**
	 * compute the offset from seq2 to seq1
	 * @param seq1
	 * @param seq2
	 */
	public static long seqOffset(long seq1, long seq2){
		if (Math.abs(seq1 - seq2) < maxOffset)
			return seq2 - seq1;

		if (seq1 < seq2)
			return seq2 - seq1 - maxSequenceNo - 1;

		return seq2 - seq1 + maxSequenceNo + 1;
	}

	/**
	 * increment by one
	 * @param seq
	 */
	public static long increment(long seq){
		return (seq == maxSequenceNo) ? 0 : seq + 1;
	}

	/**
	 * decrement by one
	 * @param seq
	 */
	public static long decrement(long seq){
		return (seq == 0) ? maxSequenceNo : seq - 1;
	}
	
	/**
	 * generates a random number between 1 and 0x3FFFFFFF (inclusive)
	 */
	public static long random(){
		return 1+new Random().nextInt(maxOffset);
	}
	
}
