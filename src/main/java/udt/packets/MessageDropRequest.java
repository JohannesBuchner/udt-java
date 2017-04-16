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


public class MessageDropRequest extends ControlPacket{
	//Bits 35-64: Message number
	
	private long msgFirstSeqNo;
	private long msgLastSeqNo;
	
	public MessageDropRequest(){
		this.controlPacketType=ControlPacketType.MESSAGE_DROP_REQUEST.ordinal();
	}
	
	public MessageDropRequest(byte[]controlInformation){
		this();
		//this.controlInformation=controlInformation;
		decode(controlInformation );
	}
	
	void decode(byte[]data){
		msgFirstSeqNo =PacketUtil.decode(data, 0);
		msgLastSeqNo =PacketUtil.decode(data, 4);
	}

	public long getMsgFirstSeqNo() {
		return msgFirstSeqNo;
	}

	public void setMsgFirstSeqNo(long msgFirstSeqNo) {
		this.msgFirstSeqNo = msgFirstSeqNo;
	}

	public long getMsgLastSeqNo() {
		return msgLastSeqNo;
	}

	public void setMsgLastSeqNo(long msgLastSeqNo) {
		this.msgLastSeqNo = msgLastSeqNo;
	}

	@Override
	public byte[] encodeControlInformation() {
		try {
			ByteArrayOutputStream bos=new ByteArrayOutputStream();
			bos.write(PacketUtil.encode(msgFirstSeqNo));
			bos.write(PacketUtil.encode(msgLastSeqNo));
			return bos.toByteArray();
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
		MessageDropRequest other = (MessageDropRequest) obj;
		if (msgFirstSeqNo != other.msgFirstSeqNo)
			return false;
		if (msgLastSeqNo != other.msgLastSeqNo)
			return false;
		return true;
	}
	 
	

}
