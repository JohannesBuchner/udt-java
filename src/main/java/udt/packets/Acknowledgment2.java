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

import udt.UDTSender;

/**
 * Ack2 is sent by the {@link UDTSender} as immediate reply to an {@link Acknowledgement}
 */
public class Acknowledgment2 extends ControlPacket{


	//the ack sequence number
	private long ackSequenceNumber ;

	public Acknowledgment2(){
		this.controlPacketType=ControlPacketType.ACK2.ordinal();	
	}

	public Acknowledgment2(long ackSeqNo,byte[]controlInformation){
		this();
		this.ackSequenceNumber=ackSeqNo;
		decode(controlInformation );
	}

	public long getAckSequenceNumber() {
		return ackSequenceNumber;
	}
	 public void setAckSequenceNumber(long ackSequenceNumber) {
		this.ackSequenceNumber = ackSequenceNumber;
	}
	 
	void decode(byte[]data){
	}

	public boolean forSender(){
		return false;
	}

	private static final byte[]empty=new byte[0];
	@Override
	public byte[] encodeControlInformation(){
		return empty;
	}
}



