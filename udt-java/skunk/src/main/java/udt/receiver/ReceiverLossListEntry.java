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

import udt.util.Util;

/**
 * an entry in the {@link ReceiverLossList}
 */
public class ReceiverLossListEntry implements Comparable<ReceiverLossListEntry> {

	private final long sequenceNumber;
	private	long lastFeedbacktime;
	private long k = 2;

	/**
	 * constructor
	 * @param sequenceNumber
	 */
	public ReceiverLossListEntry(long sequenceNumber){
		if(sequenceNumber<=0){
			throw new IllegalArgumentException("Got sequence number "+sequenceNumber);
		}
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


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (k ^ (k >>> 32));
		result = prime * result
				+ (int) (sequenceNumber ^ (sequenceNumber >>> 32));
		return result;
	}


	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ReceiverLossListEntry other = (ReceiverLossListEntry) obj;
		if (sequenceNumber != other.sequenceNumber)
			return false;
		return true;
	}

}
