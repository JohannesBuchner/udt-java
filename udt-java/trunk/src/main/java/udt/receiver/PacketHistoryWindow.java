/*********************************************************************************
 * Copyright (c) 2010 Forschungszentrum Juelich GmbH 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * (1) Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the disclaimer at the end. Redistributions in
 * binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution.
 * 
 * (2) Neither the name of Forschungszentrum Juelich GmbH nor the names of its 
 * contributors may be used to endorse or promote products derived from this 
 * software without specific prior written permission.
 * 
 * DISCLAIMER
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *********************************************************************************/

package udt.receiver;

import udt.util.CircularArray;

/**
 * A circular array that records the packet arrival times 
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
	public long getPacketArrivalSpeed(){
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
		return (long)Math.ceil(medianPacketArrivalSpeed);
	}

}
