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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
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
		SenderLossListEntry other = (SenderLossListEntry) obj;
		if (sequenceNumber != other.sequenceNumber)
			return false;
		return true;
	}

	public String toString(){
		return "lossListEntry-"+sequenceNumber;
	}
}
