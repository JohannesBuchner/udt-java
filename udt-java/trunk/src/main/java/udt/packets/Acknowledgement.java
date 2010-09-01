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

import udt.UDTReceiver;
import udt.UDTSender;

/**
 * Acknowledgement is sent by the {@link UDTReceiver} to the {@link UDTSender} to acknowledge
 * receipt of packets
 */
public class Acknowledgement extends ControlPacket {

	//the ack sequence number
	private long ackSequenceNumber ;

	//the packet sequence number to which all the previous packets have been received (excluding)
	private long ackNumber ;

	//round-trip time in microseconds(RTT)
	private long roundTripTime;
	// RTT variance
	private long roundTripTimeVariance;
	//Available buffer size (in bytes)
	private long bufferSize;
	//packet receivind rate in number of packets per second
	private long pktArrivalSpeed;
	//estimated link capacity in number of packets per second
	private long estimatedLinkCapacity;

	public Acknowledgement(){
		this.controlPacketType=ControlPacketType.ACK.ordinal();
	}

	public Acknowledgement(long ackSeqNo, byte[] controlInformation){
		this();
		this.ackSequenceNumber=ackSeqNo;
		decodeControlInformation(controlInformation);
	}

	void decodeControlInformation(byte[] data){
		ackNumber=PacketUtil.decode(data, 0);
		if(data.length>4){
			roundTripTime =PacketUtil.decode(data, 4);
			roundTripTimeVariance = PacketUtil.decode(data, 8);
			bufferSize = PacketUtil.decode(data, 12);
		}
		if(data.length>16){
			pktArrivalSpeed = PacketUtil.decode(data, 16);
			estimatedLinkCapacity = PacketUtil.decode(data, 20);
		}
	}

	@Override
	protected long getAdditionalInfo(){
		return ackSequenceNumber;
	}

	public long getAckSequenceNumber() {
		return ackSequenceNumber;
	}
	public void setAckSequenceNumber(long ackSequenceNumber) {
		this.ackSequenceNumber = ackSequenceNumber;
	}


	/**
	 * get the ack number (the number up to which all packets have been received (excluding))
	 * @return
	 */
	public long getAckNumber() {
		return ackNumber;
	}

	/**
	 * set the ack number (the number up to which all packets have been received (excluding))
	 * @param ackNumber
	 */
	public void setAckNumber(long ackNumber) {
		this.ackNumber = ackNumber;
	}

	/**
	 * get the round trip time (microseconds)
	 * @return
	 */
	public long getRoundTripTime() {
		return roundTripTime;
	}
	/**
	 * set the round trip time (in microseconds)
	 * @param RoundTripTime
	 */
	public void setRoundTripTime(long RoundTripTime) {
		roundTripTime = RoundTripTime;
	}

	/**
	 * set the variance of the round trip time (in microseconds)
	 * @param RoundTripTime
	 */
	public void setRoundTripTimeVar(long roundTripTimeVar) {
		roundTripTimeVariance = roundTripTimeVar;
	}

	public long getRoundTripTimeVar() {
		return roundTripTimeVariance;
	}

	public long getBufferSize() {
		return bufferSize;
	}

	public void setBufferSize(long bufferSiZe) {
		this.bufferSize = bufferSiZe;
	}

	public long getPacketReceiveRate() {
		return pktArrivalSpeed;
	}
	public void setPacketReceiveRate(long packetReceiveRate) {
		this.pktArrivalSpeed = packetReceiveRate;
	}


	public long getEstimatedLinkCapacity() {
		return estimatedLinkCapacity;
	}

	public void setEstimatedLinkCapacity(long estimatedLinkCapacity) {
		this.estimatedLinkCapacity = estimatedLinkCapacity;
	}

	@Override
	public byte[] encodeControlInformation(){
		try {
			ByteArrayOutputStream bos=new ByteArrayOutputStream();
			bos.write(PacketUtil.encode(ackNumber));
			bos.write(PacketUtil.encode(roundTripTime));
			bos.write(PacketUtil.encode(roundTripTimeVariance));
			bos.write(PacketUtil.encode(bufferSize));
			bos.write(PacketUtil.encode(pktArrivalSpeed));
			bos.write(PacketUtil.encode(estimatedLinkCapacity));

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
		Acknowledgement other = (Acknowledgement) obj;
		if (ackNumber != other.ackNumber)
			return false;
		if (roundTripTime != other.roundTripTime)
			return false;
		if (roundTripTimeVariance != other.roundTripTimeVariance)
			return false;
		if (bufferSize != other.bufferSize)
			return false;
		if (estimatedLinkCapacity != other.estimatedLinkCapacity)
			return false;
		if (pktArrivalSpeed != other.pktArrivalSpeed)
			return false;
		return true;
	}





}
