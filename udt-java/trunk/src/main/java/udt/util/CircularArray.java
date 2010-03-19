package udt.util;

import java.util.ArrayList;
import java.util.List;

public class CircularArray<T>{

	protected int position=0;
	protected boolean haveOverflow=false;
	//the maximum number of entries
	protected int max=1;
	
	protected List<T>circularArray;
	
	/**
	 * ArrayList von T(object's type).  The most recent value overwrite the oldest one
	 * if no more free space in the array
	 * @param size
	 */
	public CircularArray(int size){
		max=size;
		circularArray=new ArrayList<T>(size);	
	}
	
	/**
	 * Insert the specified entry at the specified position in this list.
	 * the most recent value overwrite the oldest one
	 * if no more free space in the circularArray
	 * @param entry
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
	 * @return
	 */
	public int size(){
		return circularArray.size();
	}
	
	public String toString(){
		return circularArray.toString();
	}
	
	
	
}
