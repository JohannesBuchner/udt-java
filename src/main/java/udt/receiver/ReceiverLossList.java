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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.PriorityBlockingQueue;

import udt.util.Util;

/**
 * the receiver loss list stores information about lost packets,
 * ordered by increasing sequence number.
 * 
 * @see ReceiverLossListEntry
 */
public class ReceiverLossList {

	private final PriorityBlockingQueue<ReceiverLossListEntry>backingList;
	
	public ReceiverLossList(){
		backingList = new PriorityBlockingQueue<ReceiverLossListEntry>(32);
	}
	
	public void insert(ReceiverLossListEntry entry){
		synchronized (backingList) {
			if(!backingList.contains(entry)){
				backingList.add(entry);
			}
		}
	}

	public void remove(long seqNo){
		backingList.remove(new ReceiverLossListEntry(seqNo));
	}
	
	public boolean contains(ReceiverLossListEntry obj){
		return backingList.contains(obj);
	}
	
	public boolean isEmpty(){
		return backingList.isEmpty();
	}
	
	/**
	 * read (but NOT remove) the first entry in the loss list
	 * @return
	 */
	public ReceiverLossListEntry getFirstEntry(){
		return backingList.peek();
	}
	
	public int size(){
		return backingList.size();
	}
	
	/**
	 * return all sequence numbers whose last feedback time is larger than k*RTT
	 * 
	 * @param RTT - the current round trip time
	 * @param doFeedback - true if the k parameter should be increased and the time should 
	 * be reset (using {@link ReceiverLossListEntry#feedback()} )
	 * @return
	 */
	public List<Long>getFilteredSequenceNumbers(long RTT, boolean doFeedback){
		List<Long>result=new ArrayList<Long>();
		ReceiverLossListEntry[]sorted=backingList.toArray(new ReceiverLossListEntry[0]);
		Arrays.sort(sorted);
		for(ReceiverLossListEntry e: sorted){
			if( (Util.getCurrentTime()-e.getLastFeedbackTime())>e.getK()*RTT){
				result.add(e.getSequenceNumber());
				if(doFeedback)e.feedback();
			}
		}
		return result;
	}
	
	public String toString(){
		return backingList.toString();
	}
	
	
}
