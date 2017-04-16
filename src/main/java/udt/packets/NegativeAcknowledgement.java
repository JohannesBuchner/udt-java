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

package udt.packets;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * NAK carries information about lost packets
 * 
 * loss info is described in the spec on p.15
 */
public class NegativeAcknowledgement extends ControlPacket{

	//after decoding this contains the lost sequence numbers
	List<Integer>lostSequenceNumbers;

	//this contains the loss information intervals as described on p.15 of the spec
	ByteArrayOutputStream lossInfo=new ByteArrayOutputStream();

	public NegativeAcknowledgement(){
		this.controlPacketType=ControlPacketType.NAK.ordinal();
	}

	public NegativeAcknowledgement(byte[]controlInformation){
		this();
		lostSequenceNumbers=decode(controlInformation);
	}
	
	/**
	 * decode the loss info
	 * @param lossInfo
	 */
	private List<Integer> decode(byte[]lossInfo){
		List<Integer>lostSequenceNumbers=new ArrayList<Integer>();
		ByteBuffer bb=ByteBuffer.wrap(lossInfo);
		byte[]buffer=new byte[4];
		while(bb.remaining()>0){
			//read 4 bytes
			buffer[0]=bb.get();
			buffer[1]=bb.get();
			buffer[2]=bb.get();
			buffer[3]=bb.get();
			boolean isNotSingle=(buffer[0]&128)!=0;
			//set highest bit back to 0
			buffer[0]=(byte)(buffer[0]&0x7f);
			int lost=ByteBuffer.wrap(buffer).getInt();
			if(isNotSingle){
				//get the end of the interval
				int end=bb.getInt();
				//and add all lost numbers to the result list
				for(int i=lost;i<=end;i++){
					lostSequenceNumbers.add(i);
				}
			}else{
				lostSequenceNumbers.add(lost);
			}
		}
		return lostSequenceNumbers;
	}

	/**
	 * add a single lost packet number
	 * @param singleSequenceNumber
	 */
	public void addLossInfo(long singleSequenceNumber) {
		byte[] enc=PacketUtil.encodeSetHighest(false, singleSequenceNumber);
		try{
			lossInfo.write(enc);
		}catch(IOException ignore){}
	}

	/**
	 * add an interval of lost packet numbers
	 * @param firstSequenceNumber
	 * @param lastSequenceNumber
	 */
	public void addLossInfo(long firstSequenceNumber, long lastSequenceNumber) {
		//check if we really need an interval
		if(lastSequenceNumber-firstSequenceNumber==0){
			addLossInfo(firstSequenceNumber);
			return;
		}
		//else add an interval
		byte[] enc1=PacketUtil.encodeSetHighest(true, firstSequenceNumber);
		byte[] enc2=PacketUtil.encodeSetHighest(false, lastSequenceNumber);
		try{
			lossInfo.write(enc1);
			lossInfo.write(enc2);
		}catch(IOException ignore){}
	}

	/**
	 * pack the given list of sequence numbers and add them to the loss info
	 * @param sequenceNumbers - a list of sequence numbers
	 */
	public void addLossInfo(List<Long>sequenceNumbers) {
		long start=0;
		int index=0;
		do{
			start=sequenceNumbers.get(index);
			long end=0;
			int c=0;
			do{
				c++;
				index++;
				if(index<sequenceNumbers.size()){
					end=sequenceNumbers.get(index);
				}
			}while(end-start==c);
			if(end==0){
				addLossInfo(start);
			}
			else{
				end=sequenceNumbers.get(index-1);
				addLossInfo(start, end);
			}
		}while(index<sequenceNumbers.size());
	}

	/**
	 * Return the lost packet numbers
	 * @return
	 */
	public List<Integer> getDecodedLossInfo() {
		return lostSequenceNumbers;
	}

	@Override
	public byte[] encodeControlInformation(){
		try {
			return lossInfo.toByteArray();
		} catch (Exception e) {
			// can't happen
			return null;
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		NegativeAcknowledgement other = (NegativeAcknowledgement) obj;
		
		List<Integer>thisLost=null;
		List<Integer>otherLost=null;
		
		//compare the loss info
		if(lostSequenceNumbers!=null){
			thisLost=lostSequenceNumbers;
		}else{
			thisLost=decode(lossInfo.toByteArray());
		}
		if(other.lostSequenceNumbers!=null){
			otherLost=other.lostSequenceNumbers;
		}else{
			otherLost=other.decode(other.lossInfo.toByteArray());
		}
		if(!thisLost.equals(otherLost)){
			return false;
		}

		return true;
	}


}
