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

package udt.util;

import java.util.concurrent.PriorityBlockingQueue;

/**
 * bounded queue 
 * 
 */
public class FlowWindow<E> extends PriorityBlockingQueue<E> {

	private static final long serialVersionUID=1l;

	private volatile int capacity;
	
	/**
	 * create a new flow window with the given size
	 * 
	 * @param size - the initial size of the flow window
	 */
	public FlowWindow(int size){
		super();
		this.capacity=size;
	}
	
	/**
	 * create a new flow window with the default size of 16
	 */
	public FlowWindow(){
		this(16);
	}
	
	public void setCapacity(int newSize){
		capacity=newSize;
	}
	
	public int getCapacity(){
		return capacity;
	}

	/**
	 * try to add an element to the queue, return false if it is not possible
	 */
	@Override
	public boolean offer(E e) {
		if(contains(e)){
			return true;
		}
		if(size()<capacity){
			return super.offer(e);
		}else return false;
	}	
	
	
	
}
