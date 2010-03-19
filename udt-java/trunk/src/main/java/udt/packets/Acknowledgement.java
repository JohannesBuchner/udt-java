package udt.packets;
import java.io.ByteArrayOutputStream;

import udt.UDTReceiver;
import udt.UDTSender;

/**
 * Acknowledgement is sent by the {@link UDTReceiver} to the {@link UDTSender} to acknowledge
 * receipt of packets
 */
public class Acknowledgement extends ControlPacket {
	
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
		this.contrlPktTyp=ControlPacketType.ACK.ordinal();
	}
	
	public Acknowledgement(byte[] controlInformation){
		this();
		decodeControlInformation(controlInformation);
	}
	
	void decodeControlInformation(byte[] data){
		ackNumber=PacketUtil.decode(data, 0);
		roundTripTime =PacketUtil.decode(data, 4);
		roundTripTimeVariance = PacketUtil.decode(data, 8);
		bufferSize = PacketUtil.decode(data, 12);
		pktArrivalSpeed = PacketUtil.decode(data, 16);
		estimatedLinkCapacity = PacketUtil.decode(data, 20);
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
	 * @param nexttoPrevPktSeqNO
	 */
	public void setNexttoPrevPktSeqNO(long nexttoPrevPktSeqNO) {
		ackNumber = nexttoPrevPktSeqNO;
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
