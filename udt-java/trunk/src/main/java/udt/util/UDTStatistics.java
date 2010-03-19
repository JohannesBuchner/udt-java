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
	private long packetArrivalRate;
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
