package udt;

import java.util.List;
import java.util.logging.Logger;

import udt.util.Util;

public class UDTCongestionControl {

	private static final Logger logger=Logger.getLogger(ClientSession.class.getName());
	//The UDT entity that this congestion algorithm is bound
	private final UDTSession session;
	private long roundTripTime;

	private long packetArrivalRate=100;
	private long estimatedLinkCapacity;
	long maxControlWindowSize=128;
	long maxSegmentSize=32768;

	// Periodical timer to send an ACK, in milliseconds
	private long aCKPeriod; 
	// How many packets to send one ACK, in packets
	private long aCKInterval;
	// if the RTO value is defined by users
	private boolean userDefinedRTO; 
	// Retransmission time out(RTO) value, microseconds
	private long retransmissionTimeOut;                 

	// Packet sending period = packetsendInterval, in microseconds
	private double packetSendingPeriod=0.001;              
	// Congestion window size, in packets
	private long congestionWindowSize=16;
	// estimated bandwidth, packets per second
	private long estimatedBandwidth;	
	//number of packets to be increase
	private double numOfIncreasingPacket;		
	//last rate increase time
	long lastRateIncreaseTime;
	/*if in slow start phase*/
	boolean slowStartPhase=true;
	
	/*last ACKed seq no*/
	long lastAckSeqNumber=-1;
	/*max packet seq. no. sent out when last decrease happened*/
	private	long lastDecreaseSeqNo;

	//value of packetSendPeriod when last decrease happened
	long lastDecreasePeriod;
	//NAK counter
	long nACKCount=0;
	//number of decreases in a congestion epoch
	long congestionEpochDecreaseCount;
	//random threshold on decrease by number of loss events
	long decreaseRandom; 
	//average number of NAKs per congestion
	long averageNACKNum;

	public UDTCongestionControl(UDTSession session){
		this.session=session;
		init();
	}

	/**
	 * Callback function to be called (only) at the start of a UDT connection.
	 * when the UDT socket is conected 
	 */
	public void init() {
		lastRateIncreaseTime= Util.getCurrentTime();//???
		slowStartPhase = true;
		congestionWindowSize = 16;
		packetSendingPeriod=0;
		decreaseRandom =1;
		averageNACKNum=1;
		nACKCount=1;
		congestionEpochDecreaseCount=1;
		//lastDecreasePeriod=1;//???
		lastDecreaseSeqNo= session.getInitialSequenceNumber()-1;
	}

	public void setRTT(long rtt, long rttVar){
		this.roundTripTime=rtt;
	}

	public void setPacketArrivalRate(long rate, long linkCapacity){
		this.packetArrivalRate=rate;
		this.estimatedLinkCapacity=linkCapacity;
	}

	/**
	 * Inter-packet interval in seconds
	 * @return 
	 */
	public double getSendInterval(){
		return packetSendingPeriod ;
	}
	/**
	 * congestionWindowSize
	 * @return
	 */
	protected long getCongestionWindowSize(){
		return congestionWindowSize;
	}

	/**
	 * Callback function to be called when an ACK packet is received.
	 * @param ackSeqno: the data sequence number acknowledged by this ACK.
	 * see spec. page(16-17)
	 */
	public void onACK(long ackSeqno){
		//the fixed size of a UDT packet 
		long maxSegmentSize=UDPEndPoint.DATAGRAM_SIZE;
		//1.if it is  in slow start phase,set the congestion window size the product
		//of packet arrival rate and(rtt +SYN)
		double A=packetArrivalRate*(roundTripTime+0.01);
		if(slowStartPhase){
			congestionWindowSize=16;
			slowStartPhase=false;
			return;
		}else{
			congestionWindowSize=(long)A+16;
		}

		//4.compute the number of sent packets to be increase in the next SYN period
		//and update the send intervall
		if(estimatedLinkCapacity<= packetArrivalRate){
			numOfIncreasingPacket= 1/maxSegmentSize;
		}else{
			numOfIncreasingPacket=computeNumOfIncreasingPacket();
		}
		//4.update the send period :
		packetSendingPeriod=(packetSendingPeriod*Util.getSYNTime())/
					(packetSendingPeriod*numOfIncreasingPacket+Util.getSYNTime());
	}

	public double computeNumOfIncreasingPacket (){
		long B,C,S;
		B=estimatedLinkCapacity;
		C=packetArrivalRate;
		S=maxSegmentSize;
		double Beta=0.0000015/S;
		double logBase10=Math.log10( S*(B-C)*8 );
		double power10 = Math.pow( 10.0,Math.ceil (logBase10) )* Beta;
		double inc = Math.max(power10, 1/S);
		return inc;
	}
	
	/**
	 *  Callback function to be called when a loss report is received.
	 * @param lossInfo:list of sequence number of packets, in the format describled in packet.cpp.
	 */
	public void onNAK(List<Integer>lossInfo){
		long firstBiggestlossSeqNo=lossInfo.get(lossInfo.size()-1);
		long currentMaxSequenceNumber=session.getSocket().getSender().getCurrentSequenceNumber();
		lastAckSeqNumber = currentMaxSequenceNumber;
		nACKCount++;
		/*1) If it is in slow start phase, set inter-packet interval to 
      	   1/recvrate. Slow start ends. Stop. */
		if(slowStartPhase){
			packetSendingPeriod = 1/packetArrivalRate;
			slowStartPhase = false;
			return;
		}

		//start new congestion epoch
		if(firstBiggestlossSeqNo>lastDecreaseSeqNo){
			// 2)If this NAK starts a new congestion epoch
			// -increase inter-packet interval
			packetSendingPeriod = Math.ceil(packetSendingPeriod*1.125);
			// -Update AvgNAKNum(the average number of NAKs per congestion)
			averageNACKNum = (int)Math.ceil(averageNACKNum*0.875 + nACKCount*0.125);
			// -reset NAKCount to 1, 
			nACKCount=1;
			/* - compute DecRandom to a random (average distribution) number between 1 and AvgNAKNum.. */
			decreaseRandom =(int)Math.ceil((averageNACKNum-1)*Math.random()+1);
			// -Update LastDecSeq
			lastDecreaseSeqNo = currentMaxSequenceNumber;
			// -Stop.
			return;
		}

		//* 3) If DecCount <= 5, and NAKCount == DecCount * DecRandom: 
		if(congestionEpochDecreaseCount<=5 && 
				nACKCount==congestionEpochDecreaseCount*decreaseRandom){
			// a. Update SND period: SND = SND * 1.125; 
			packetSendingPeriod = Math.ceil(packetSendingPeriod*1.125);
			// b. Increase DecCount by 1; 
			congestionEpochDecreaseCount++;
			// c. Record the current largest sent sequence number (LastDecSeq).
			lastDecreaseSeqNo= currentMaxSequenceNumber;
			return;
		}
	}

	/**
	 * Callback function to be called when a timeout event occurs
	 */
	public void onTimeout(){}

	/**
	 * Callback function to be called when a data is sent.
	 * @param packetSeqNo: the data sequence number.
	 */
	public void onPacketSend(long packetSeqNo){}

	/**
	 * Callback function to be called when a data is received.
	 * @param packetSeqNo: the data sequence number.
	 */
	public void onPacketReceive(long packetSeqNo){}
	/**
	 * Callback function to be called when a UDT connection is closed.
	 */
	public void close(){}


}
