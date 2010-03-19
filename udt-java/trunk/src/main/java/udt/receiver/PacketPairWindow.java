package udt.receiver;

import udt.util.CircularArray;

/**
 * a circular array that records time intervals between two data packets
 * @see {@link CircularArray}
 * 
 */
public class PacketPairWindow extends CircularArray<Long>{
	
	/**
	 * construct a new packet pair window with the given size
	 * 
	 * @param size
	 */
	public PacketPairWindow(int size){
		super(size);
	}
	
	/**
	 * compute the median packet pair interval of the last
	 * 16 packet pair intervals (PI).
	 * (see specification section 6.2, page 12)
	 * @return time interval in microseconds
	 */
	public double computeMedianTimeInterval(){
		int num=haveOverflow?max:Math.min(max, position);
		double total=0;
		for(int i=0; i<num;i++){
			total+=circularArray.get(i).doubleValue();	
		}
		return total/num;
	}
	
	/**
	 * compute the estimated linK capacity using the values in
	 * packet pair window
	 * @return number of  packets per second
	 */
	public double getEstimatedLinkCapacity(){
		return 1e6/computeMedianTimeInterval();
	}
}
