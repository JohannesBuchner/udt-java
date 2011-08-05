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
 * a circular array that records time intervals between two probing data packets.
 * It is used to determine the estimated link capacity.
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
		double median=0;
		for(int i=0; i<num;i++){
			median+=circularArray.get(i).doubleValue();	
		}
		median=median/num;
		
		//median filtering
		double upper=median*8;
		double lower=median/8;
		double total = 0;
		double val=0;
		int count=0;
		for(int i=0; i<num;i++){
			val=circularArray.get(i).doubleValue();
			if(val<upper && val>lower){
				total+=val;
				count++;
			}
		}
		double res=total/count;
		return res;
	}
	
	/**
	 * compute the estimated linK capacity using the values in
	 * packet pair window
	 * @return number of packets per second
	 */
	public long getEstimatedLinkCapacity(){
		long res=(long)Math.ceil(1000000/computeMedianTimeInterval());
		return res;
	}
}
