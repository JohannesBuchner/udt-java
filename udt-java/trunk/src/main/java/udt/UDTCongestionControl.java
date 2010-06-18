package udt;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import udt.util.UDTStatistics;
import udt.util.Util;

/**
 * default UDT congestion control.<br/>
 * 
 * The algorithm is adapted from the C++ reference implementation.
 */
public class UDTCongestionControl implements CongestionControl {

	private static final Logger logger=Logger.getLogger(UDTCongestionControl.class.getName());

	protected final UDTSession session;

	protected final UDTStatistics statistics;

	//round trip time in microseconds
	protected long roundTripTime=0;

	//rate in packets per second
	protected long packetArrivalRate=0;

	//link capacity in packets per second
	protected long estimatedLinkCapacity=0;

	// Packet sending period = packet send interval, in microseconds
	protected double packetSendingPeriod=1;              

	// Congestion window size, in packets
	protected double congestionWindowSize=16;

	/*if in slow start phase*/
	private boolean slowStartPhase=true;

	/*last ACKed seq no*/
	private long lastAckSeqNumber=-1;

	/*max packet seq. no. sent out when last decrease happened*/
	private	long lastDecreaseSeqNo;

	//NAK counter
	private long nACKCount=1;

	//number of decreases in a congestion epoch
	long decCount=1;

	//random threshold on decrease by number of loss events
	long decreaseRandom=1; 

	//average number of NAKs per congestion
	long averageNACKNum;

	//this flag avoids immediate rate increase after a NAK
	private boolean loss=false;

	//if larger than 0, the receiver should acknowledge every n'th packet
	protected long ackInterval=-1;
	
	public UDTCongestionControl(UDTSession session){
		this.session=session;
		this.statistics=session.getStatistics();
		lastDecreaseSeqNo=session.getInitialSequenceNumber()-1;
	}

	/* (non-Javadoc)
	 * @see udt.CongestionControl#init()
	 */
	public void init() {
		
	}

	/* (non-Javadoc)
	 * @see udt.CongestionControl#setRTT(long, long)
	 */
	public void setRTT(long rtt, long rttVar){
		this.roundTripTime=rtt;
	}

	/* (non-Javadoc)
	 * @see udt.CongestionControl#setPacketArrivalRate(long, long)
	 */
	public void updatePacketArrivalRate(long rate, long linkCapacity){
		//see spec p. 14.
		if(packetArrivalRate>0)packetArrivalRate=(packetArrivalRate*7+rate)/8;
		else packetArrivalRate=rate;
		if(estimatedLinkCapacity>0)estimatedLinkCapacity=(estimatedLinkCapacity*7+linkCapacity)/8;
		else estimatedLinkCapacity=linkCapacity;
	}

	public long getPacketArrivalRate() {
		return packetArrivalRate;
	}

	public long getEstimatedLinkCapacity() {
		return estimatedLinkCapacity;
	}

	/* (non-Javadoc)
	 * @see udt.CongestionControl#getSendInterval()
	 */
	public double getSendInterval(){
		return packetSendingPeriod;
	}

	public long getAckInterval(){
		return ackInterval;
	}
	public void setAckInterval(long ackInterval){
		this.ackInterval=ackInterval;
		if(session.getSocket()!=null && session.getSocket().getReceiver()!=null){
			session.getSocket().getReceiver().setAckInterval(ackInterval);
		}
	}
	
	/**
	 * congestionWindowSize
	 * @return
	 */
	public double getCongestionWindowSize(){
		return congestionWindowSize;
	}

	/* (non-Javadoc)
	 * @see udt.CongestionControl#onACK(long)
	 */
	public void onACK(long ackSeqno){
		//increase window during slow start
		if(slowStartPhase){
			congestionWindowSize+=ackSeqno-lastAckSeqNumber;
			lastAckSeqNumber = ackSeqno;
			//but not beyond a maximum size
			if(congestionWindowSize>session.getFlowWindowSize()){
				slowStartPhase=false;
				if(packetArrivalRate>0){
					packetSendingPeriod=1000000.0/packetArrivalRate;
				}
				else{
					packetSendingPeriod=(double)congestionWindowSize/(roundTripTime+Util.getSYNTimeD());
				}
			}

		}else{
			//1.if it is  not in slow start phase,set the congestion window size 
			//to the product of packet arrival rate and(rtt +SYN)
			double A=packetArrivalRate/1000000.0*(roundTripTime+Util.getSYNTimeD());
			congestionWindowSize=(long)A+16;
			if(logger.isLoggable(Level.FINER)){
				logger.finer("receive rate "+packetArrivalRate+" rtt "+roundTripTime+" set to window size: "+(A+16));
			}
		}

		//no rate increase during slow start
		if(slowStartPhase)return;

		//no rate increase "immediately" after a NAK
		if(loss){
			loss=false;
			return;
		}

		//4. compute the increase in sent packets for the next SYN period
		double numOfIncreasingPacket=computeNumOfIncreasingPacket();

		//5. update the send period
		double factor=Util.getSYNTimeD()/(packetSendingPeriod*numOfIncreasingPacket+Util.getSYNTimeD());
		packetSendingPeriod=factor*packetSendingPeriod;
		//packetSendingPeriod=0.995*packetSendingPeriod;

		statistics.setSendPeriod(packetSendingPeriod);
	}

	private final long PS=UDPEndPoint.DATAGRAM_SIZE;
	private final double BetaDivPS=0.0000015/PS;

	//see spec page 16
	private double computeNumOfIncreasingPacket (){
		//difference between link capacity and sending speed, in packets per second 
		double remaining=estimatedLinkCapacity-1000000.0/packetSendingPeriod;

		if(remaining<=0){
			return 1.0/UDPEndPoint.DATAGRAM_SIZE;
		}
		else{
			double exp=Math.ceil(Math.log10(remaining*PS*8));
			double power10 = Math.pow( 10.0, exp)* BetaDivPS;
			return Math.max(power10, 1/PS);
		}
	}

	/* (non-Javadoc)
	 * @see udt.CongestionControl#onNAK(java.util.List)
	 */
	public void onLoss(List<Integer>lossInfo){
		loss=true;
		long firstBiggestlossSeqNo=lossInfo.get(0);
		nACKCount++;
		/*1) If it is in slow start phase, set inter-packet interval to 
      	   1/recvrate. Slow start ends. Stop. */
		if(slowStartPhase){
			if(packetArrivalRate>0){
				packetSendingPeriod = 100000.0/packetArrivalRate;
			}
			else{
				packetSendingPeriod=congestionWindowSize/(roundTripTime+Util.getSYNTime());
			}
			slowStartPhase = false;
			return;
		}

		long currentMaxSequenceNumber=session.getSocket().getSender().getCurrentSequenceNumber();
		// 2)If this NAK starts a new congestion epoch
		if(firstBiggestlossSeqNo>lastDecreaseSeqNo){
			// -increase inter-packet interval
			packetSendingPeriod = Math.ceil(packetSendingPeriod*1.125);
			// -Update AvgNAKNum(the average number of NAKs per congestion)
			averageNACKNum = (int)Math.ceil(averageNACKNum*0.875 + nACKCount*0.125);
			// -reset NAKCount and DecCount to 1, 
			nACKCount=1;
			decCount=1;
			/* - compute DecRandom to a random (average distribution) number between 1 and AvgNAKNum */
			decreaseRandom =(int)Math.ceil((averageNACKNum-1)*Math.random()+1);
			// -Update LastDecSeq
			lastDecreaseSeqNo = currentMaxSequenceNumber;
			// -Stop.
		}
		//* 3) If DecCount <= 5, and NAKCount == DecCount * DecRandom: 
		else if(decCount<=5 && nACKCount==decCount*decreaseRandom){
			// a. Update SND period: SND = SND * 1.125; 
			packetSendingPeriod = Math.ceil(packetSendingPeriod*1.125);
			// b. Increase DecCount by 1; 
			decCount++;
			// c. Record the current largest sent sequence number (LastDecSeq).
			lastDecreaseSeqNo= currentMaxSequenceNumber;
		}
		
		statistics.setSendPeriod(packetSendingPeriod);
		return;
	}

	/* (non-Javadoc)
	 * @see udt.CongestionControl#onTimeout()
	 */
	public void onTimeout(){}

	/* (non-Javadoc)
	 * @see udt.CongestionControl#onPacketSend(long)
	 */
	public void onPacketSend(long packetSeqNo){}

	/* (non-Javadoc)
	 * @see udt.CongestionControl#onPacketReceive(long)
	 */
	public void onPacketReceive(long packetSeqNo){}
	/* (non-Javadoc)
	 * @see udt.CongestionControl#close()
	 */
	public void close(){}


}
