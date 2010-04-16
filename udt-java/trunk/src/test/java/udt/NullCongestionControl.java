package udt;

import java.util.List;

public class NullCongestionControl implements CongestionControl {

	private final UDTSession session;
	
	public NullCongestionControl(UDTSession session){
		this.session=session;
	}
	
	public void close() {
	}

	public long getCongestionWindowSize() {
		return Long.MAX_VALUE;
	}

	public double getSendInterval() {
		return 0;
	}

	public void init() {
	}

	public void onACK(long ackSeqno) {
	}

	public void onNAK(List<Integer> lossInfo) {
	}

	public void onPacketReceive(long packetSeqNo) {
	}

	public void onPacketSend(long packetSeqNo) {
	}

	public void onTimeout() {
	}

	public void setPacketArrivalRate(long rate, long linkCapacity) {
	}

	public void setRTT(long rtt, long rttVar) {
	}

}
