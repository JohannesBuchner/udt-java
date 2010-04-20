package udt;

import java.util.List;

/**
 * congestion control interface
 */
public interface CongestionControl {

	/**
	 * Callback function to be called (only) at the start of a UDT connection.
	 * when the UDT socket is conected 
	 */
	public abstract void init();

	/**
	 * set roundtrip time and associated variance
	 */
	public abstract void setRTT(long rtt, long rttVar);

	/**
	 * update packet arrival rate and link capacity with the
	 * values received in an ACK packet
	 * @param rate
	 * @param linkCapacity
	 */
	public abstract void updatePacketArrivalRate(long rate, long linkCapacity);

	public long getPacketArrivalRate();
	
	public long getEstimatedLinkCapacity();

	
	/**
	 * Inter-packet interval in seconds
	 * @return 
	 */
	public abstract double getSendInterval();

	/**
	 * get the congestion window size
	 */
	public abstract long getCongestionWindowSize();
	
	/**
	 * Callback function to be called when an ACK packet is received.
	 * @param ackSeqno: the data sequence number acknowledged by this ACK.
	 * see spec. page(16-17)
	 */
	public abstract void onACK(long ackSeqno);

	/**
	 *  Callback function to be called when a loss report is received.
	 * @param lossInfo:list of sequence number of packets, in the format describled in packet.cpp.
	 */
	public abstract void onNAK(List<Integer> lossInfo);

	/**
	 * Callback function to be called when a timeout event occurs
	 */
	public abstract void onTimeout();

	/**
	 * Callback function to be called when a data packet is sent.
	 * @param packetSeqNo: the data sequence number.
	 */
	public abstract void onPacketSend(long packetSeqNo);

	/**
	 * Callback function to be called when a data packet is received.
	 * @param packetSeqNo: the data sequence number.
	 */
	public abstract void onPacketReceive(long packetSeqNo);

	/**
	 * Callback function to be called when a UDT connection is closed.
	 */
	public abstract void close();

}