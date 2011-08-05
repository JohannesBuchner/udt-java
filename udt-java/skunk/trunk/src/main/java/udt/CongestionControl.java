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
	 * @param rtt - round trip time in microseconds
	 * @param rttVar - round trip time variance in microseconds
	 */
	public abstract void setRTT(long rtt, long rttVar);

	/**
	 * update packet arrival rate and link capacity with the
	 * values received in an ACK packet
	 * @param rate - packet rate in packets per second
	 * @param linkCapacity - estimated link capacity in packets per second
	 */
	public abstract void updatePacketArrivalRate(long rate, long linkCapacity);

	/**
	 * get the current value of the packet arrival 
	 */
	public long getPacketArrivalRate();
	
	/**
	 * get the current value of the estimated link capacity 
	 */
	public long getEstimatedLinkCapacity();
	
	/**
	 * get the current value of the inter-packet interval in microseconds
	 */
	public abstract double getSendInterval();

	/**
	 * get the congestion window size
	 */
	public abstract double getCongestionWindowSize();
	
	/**
	 * get the ACK interval. If larger than 0, the receiver should acknowledge
	 * every n'th packet
	 */
	public abstract long getAckInterval();
	
	/**
	 * set the ACK interval. If larger than 0, the receiver should acknowledge
	 * every n'th packet
	 */
	public abstract void setAckInterval(long ackInterval);
	
	/**
	 * Callback function to be called when an ACK packet is received.
	 * @param ackSeqno - the data sequence number acknowledged by this ACK.
	 * see spec. page(16-17)
	 */
	public abstract void onACK(long ackSeqno);

	/**
	 * Callback function to be called when a loss report is received.
	 * @param lossInfo - list of sequence number of packets
	 */
	public abstract void onLoss(List<Integer> lossInfo);

	/**
	 * Callback function to be called when a timeout event occurs
	 */
	public abstract void onTimeout();

	/**
	 * Callback function to be called when a data packet is sent.
	 * @param packetSeqNo - the data packet sequence number
	 */
	public abstract void onPacketSend(long packetSeqNo);

	/**
	 * Callback function to be called when a data packet is received.
	 * @param packetSeqNo - the data packet sequence number.
	 */
	public abstract void onPacketReceive(long packetSeqNo);

	/**
	 * Callback function to be called when a UDT connection is closed.
	 */
	public abstract void close();

}