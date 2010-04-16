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

import java.util.ArrayList;
import java.util.List;

/**
 * Circular array: the most recent value overwrites the oldest one if there is no more free 
 * space in the array
 */
public class CircularArray<T>{

	protected int position=0;
	
	protected boolean haveOverflow=false;
	
	protected final int max;
	
	protected final List<T>circularArray;
	
	/**
	 * Create a new circularArray of the given size
	 * 
	 * @param size
	 */
	public CircularArray(int size){
		max=size;
		circularArray=new ArrayList<T>(size);	
	}
	
	/**
	 * add an entry
	 */
	public void add(T entry){
		if(position>=max){
			position=0;
			haveOverflow=true;
		}
		if(circularArray.size()>position){
			circularArray.remove(position);
		}
		circularArray.add(position, entry);
		position++;
	}
	
	/**
	 * Returns the number of elements in this list 
	 */
	public int size(){
		return circularArray.size();
	}
	
	public String toString(){
		return circularArray.toString();
	}

}
