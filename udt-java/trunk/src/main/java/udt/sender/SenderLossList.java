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
import java.util.LinkedList;

/**
 * stores the sequence number of the lost packets in increasing order
 */
public class SenderLossList {

	private final LinkedList<Long>backingList;

	/**
	 * create a new sender lost list
	 */
	public SenderLossList(){
		backingList = new LinkedList<Long>();
	}

	public void insert(Long obj){
		synchronized (backingList) {
			if(!backingList.contains(obj)){
				for(int i=0;i<backingList.size();i++){
					if(obj<backingList.getFirst()){
						backingList.add(i,obj);	
						return;
					}
				}
				backingList.add(obj);
			}
		}
	}

	/**
	 * retrieves the loss list entry with the lowest sequence number
	 */
	public Long getFirstEntry(){
		synchronized(backingList){
			return backingList.poll();
		}
	}

	public boolean isEmpty(){
		return backingList.isEmpty();
	}

	public int size(){
		return backingList.size();
	}

	public String toString(){
		synchronized (backingList) {
			return backingList.toString();	
		}
	}
}
