package udt.receiver;

import udt.util.CircularArray;



/**
 * A circular array that records the packet arrival times 
 *   
 * 
 * 
 */
public class PacketHistoryWindow extends CircularArray<Long>{

	/**
	 * create a new PacketHistoryWindow of the given size 
	 * @param size
	 */
	public PacketHistoryWindow(int size){
		super(size);
	}

	/**
	 * compute the packet arrival speed
	 * (see specification section 6.2, page 12)
	 * @return the current value
	 */
	public double getPacketArrivalSpeed(){
		if(!haveOverflow)return 0;
		int num=max-1;
		double AI;
		double medianPacketArrivalSpeed;
		double total=0;
		int count=0;
		long[]intervals=new long[num];
		int pos=position-1;
		if(pos<0)pos=num;
		do{
			long upper=circularArray.get(pos);
			pos--;
			if(pos<0)pos=num;
			long lower=circularArray.get(pos);
			long interval=upper-lower;
			intervals[count]=interval;
			total+=interval;
			count++;
		}while(count<num);
		//compute median
		AI=total / num;
		//compute the actual value, filtering out intervals between AI/8 and AI*8
		count=0;
		total=0;
		for(long l: intervals){
			if(l>AI/8 && l<AI*8){
				total+=l;
				count++;
			}
		}
		if(count>8){
			medianPacketArrivalSpeed=1e6*count/total;
		}
		else{
			medianPacketArrivalSpeed=0; 
		}
		return medianPacketArrivalSpeed;
	}

}
