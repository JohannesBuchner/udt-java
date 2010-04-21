package udt;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import udt.util.UDTStatistics;
import udt.util.Util;

public class UDTCongestionControl implements CongestionControl {

	private static final Logger logger=Logger.getLogger(UDTCongestionControl.class.getName());

	private final UDTSession session;
	
	private final UDTStatistics statistics;
	
	//round trip time in microseconds
	private long roundTripTime=2*Util.getSYNTime();
	
	//rate in packets per second
	private long packetArrivalRate=0;
	
	//link capacity in packets per second
	private long estimatedLinkCapacity=0;
	
	long maxControlWindowSize=128;

	// Packet sending period = packet send interval, in microseconds
	private double packetSendingPeriod=1;              
	// Congestion window size, in packets
	private long congestionWindowSize=16;
	
	//number of packets to be increased in the next SYN period
	private double numOfIncreasingPacket;		
	
	//last rate increase time (microsecond value)
	long lastRateIncreaseTime=Util.getCurrentTime();
	
	/*if in slow start phase*/
	boolean slowStartPhase=true;
	
	/*last ACKed seq no*/
	long lastAckSeqNumber=-1;
	
	/*max packet seq. no. sent out when last decrease happened*/
	private	long lastDecreaseSeqNo;

	//value of packetSendPeriod when last decrease happened
	long lastDecreasePeriod;
	
	//NAK counter
	long nACKCount=1;
	
	//number of decreases in a congestion epoch
	long decCount=1;
	
	//random threshold on decrease by number of loss events
	long decreaseRandom=1; 
	
	//average number of NAKs per congestion
	long averageNACKNum;

	boolean loss=false;
	
	public UDTCongestionControl(UDTSession session){
		this.session=session;
		this.statistics=session.getStatistics();
		lastDecreaseSeqNo=session.getInitialSequenceNumber()-1;
		init();
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
	
	/**
	 * congestionWindowSize
	 * @return
	 */
	public long getCongestionWindowSize(){
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
				System.out.println("slow start ends on ACK");
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

		//no rate increase in slow start
		if(slowStartPhase)return;
		
		if(loss){
			loss=false;
			return;
		}
		
		//4.compute the number of sent packets to be increase in the next SYN period
		//and update the send intervall
		numOfIncreasingPacket=computeNumOfIncreasingPacket();
		
		//4.update the send period :
		double factor=Util.getSYNTimeD()/(packetSendingPeriod*numOfIncreasingPacket+Util.getSYNTimeD());
		packetSendingPeriod=factor*packetSendingPeriod;
		//packetSendingPeriod=0.995*packetSendingPeriod;
		//System.out.println("dec snd factor "+factor+" to "+packetSendingPeriod);
		
		statistics.setSendPeriod(packetSendingPeriod);
	}

	private final long PS=UDPEndPoint.DATAGRAM_SIZE;
	private final double BetaDivPS=0.0000015/PS;
	
	//see spec page 16
	private double computeNumOfIncreasingPacket (){
		//link capacity and sending speed, in packets per second 
		double B=estimatedLinkCapacity;
		double C=1000000.0/packetSendingPeriod;
		
		if(B<=C)return 1.0/UDPEndPoint.DATAGRAM_SIZE;
		
		double exp=Math.ceil(Math.log10((B-C)*PS*8));
		double power10 = Math.pow( 10.0, exp)* BetaDivPS;
		double inc = Math.max(power10, 1/PS);
		return inc;
	}
	
	/* (non-Javadoc)
	 * @see udt.CongestionControl#onNAK(java.util.List)
	 */
	public void onNAK(List<Integer>lossInfo){
		loss=true;
		long firstBiggestlossSeqNo=lossInfo.get(lossInfo.size()-1);
		
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
			statistics.setSendPeriod(packetSendingPeriod);
		}

		//* 3) If DecCount <= 5, and NAKCount == DecCount * DecRandom: 
		if(decCount<=5 && nACKCount==decCount*decreaseRandom){
			// a. Update SND period: SND = SND * 1.125; 
			packetSendingPeriod = Math.ceil(packetSendingPeriod*1.125);
			// b. Increase DecCount by 1; 
			decCount++;
			// c. Record the current largest sent sequence number (LastDecSeq).
			lastDecreaseSeqNo= currentMaxSequenceNumber;
			statistics.setSendPeriod(packetSendingPeriod);
			return;
		}
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
