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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class is used to keep some statistics about a UDT connection. 
 * It also allows to compute a MD5 hash over the received data
 */
public class UDTStatistics {

	private final AtomicInteger numberOfSentDataPackets=new AtomicInteger(0);
	private final AtomicInteger numberOfReceivedDataPackets=new AtomicInteger(0);
	private final AtomicInteger numberOfDuplicateDataPackets=new AtomicInteger(0);
	private final AtomicInteger numberOfNAKSent=new AtomicInteger(0);
	private final AtomicInteger numberOfNAKReceived=new AtomicInteger(0);
	private final AtomicInteger numberOfRetransmittedDataPackets=new AtomicInteger(0);
	private final AtomicInteger numberOfACKSent=new AtomicInteger(0);
	private final String componentDescription;

	private long roundTripTime;
	private long roundTripTimeVariance;
	private volatile long packetArrivalRate;
	private long estimatedLinkCapacity;
	
	private MessageDigest digest;

	public UDTStatistics(String componentDescription){
		this.componentDescription=componentDescription;
		try{
			digest=MessageDigest.getInstance("MD5");
		}catch(NoSuchAlgorithmException na){
			digest=null;
		}
	}

	public int getNumberOfSentDataPackets() {
		return numberOfSentDataPackets.get();
	}
	public int getNumberOfReceivedDataPackets() {
		return numberOfReceivedDataPackets.get();
	}
	public int getNumberOfDuplicateDataPackets() {
		return numberOfDuplicateDataPackets.get();
	}
	public int getNumberOfNAKSent() {
		return numberOfNAKSent.get();
	}
	public int getNumberOfNAKReceived() {
		return numberOfNAKReceived.get();
	}
	public int getNumberOfRetransmittedDataPackets() {
		return numberOfRetransmittedDataPackets.get();
	}
	public int getNumberOfACKSent() {
		return numberOfACKSent.get();
	}
	public void incNumberOfSentDataPackets() {
		numberOfSentDataPackets.incrementAndGet();
	}
	public void incNumberOfReceivedDataPackets() {
		numberOfReceivedDataPackets.incrementAndGet();
	}
	public void incNumberOfDuplicateDataPackets() {
		numberOfDuplicateDataPackets.incrementAndGet();
	}
	public void incNumberOfNAKSent() {
		numberOfNAKSent.incrementAndGet();
	}
	public void incNumberOfNAKReceived() {
		numberOfNAKReceived.incrementAndGet();
	}
	public void incNumberOfRetransmittedDataPackets() {
		numberOfRetransmittedDataPackets.incrementAndGet();
	}

	public void incNumberOfACKSent() {
		numberOfACKSent.incrementAndGet();
	}

	public void setRTT(long rtt, long rttVar){
		this.roundTripTime=rtt;
		this.roundTripTimeVariance=rttVar;
	}

	public void setPacketArrivalRate(long rate, long linkCapacity){
		this.packetArrivalRate=rate;
		this.estimatedLinkCapacity=linkCapacity;
	}
	
	public void updateReadDataMD5(byte[]data){
		digest.update(data);
	}

	public String getDigest(){
		return hexString(digest);
	}
	
	public long getPacketArrivalRate(){
		return packetArrivalRate;
	}
	
	public String toString(){
		StringBuilder sb=new StringBuilder();
		sb.append("Statistics for ").append(componentDescription).append("\n");
		sb.append("Sent data packets: ").append(getNumberOfSentDataPackets()).append("\n");
		sb.append("Received data packets: ").append(getNumberOfReceivedDataPackets()).append("\n");
		sb.append("Duplicate data packets: ").append(getNumberOfDuplicateDataPackets()).append("\n");
		sb.append("NAK received: ").append(getNumberOfNAKReceived()).append("\n");
		sb.append("Retransmitted data: ").append(getNumberOfNAKReceived()).append("\n");
		sb.append("NAK sent: ").append(getNumberOfNAKSent()).append("\n");
		sb.append("ACK sent: ").append(getNumberOfACKSent()).append("\n");
		if(roundTripTime>0){
			sb.append("RTT ").append(roundTripTime).append(" var. ").append(roundTripTimeVariance).append("\n");
		}
		if(packetArrivalRate>0){
			sb.append("Packet rate: ").append(packetArrivalRate).append("/sec., link capacity: ").append(estimatedLinkCapacity).append("/sec.\n");
		}
		return sb.toString();
	}

	public static String hexString(MessageDigest digest){
		byte[] messageDigest = digest.digest();
		StringBuilder hexString = new StringBuilder();
		for (int i=0;i<messageDigest.length;i++) {
			String hex = Integer.toHexString(0xFF & messageDigest[i]); 
			if(hex.length()==1)hexString.append('0');
			hexString.append(hex);
		}
		return hexString.toString();
	}

}
