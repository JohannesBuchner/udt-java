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

package udt;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import udt.packets.Acknowledgement;
import udt.packets.Acknowledgment2;
import udt.packets.ControlPacket;
import udt.packets.DataPacket;
import udt.packets.KeepAlive;
import udt.packets.NegativeAcknowledgement;
import udt.packets.Shutdown;
import udt.packets.ControlPacket.ControlPacketType;
import udt.receiver.AckHistoryEntry;
import udt.receiver.AckHistoryWindow;
import udt.receiver.PacketHistoryWindow;
import udt.receiver.PacketPairWindow;
import udt.receiver.ReceiverLossList;
import udt.receiver.ReceiverLossListEntry;
import udt.util.MeanValue;
import udt.util.SequenceNumber;
import udt.util.UDTStatistics;
import udt.util.UDTThreadFactory;
import udt.util.Util;

/**
 * receiver part of a UDT entity
 * @see UDTSender
 */
public class UDTReceiver {

	private static final Logger logger=Logger.getLogger(UDTReceiver.class.getName());

	private final UDPEndPoint endpoint;

	private final UDTSession session;

	private final UDTStatistics statistics;

	//record seqNo of detected lostdata and latest feedback time
	private final ReceiverLossList receiverLossList;

	//record each sent ACK and the sent time 
	private final AckHistoryWindow ackHistoryWindow;

	//Packet history window that stores the time interval between the current and the last seq.
	private final PacketHistoryWindow packetHistoryWindow;

	//for storing the arrival time of the last received data packet
	private volatile long lastDataPacketArrivalTime=0;

	//largest received data packet sequence number(LRSN)
	private volatile long largestReceivedSeqNumber=0;

	//ACK event related

	//last Ack number
	private long lastAckNumber=0;

	//largest Ack number ever acknowledged by ACK2
	private volatile long largestAcknowledgedAckNumber=-1;

	//EXP event related

	//a variable to record number of continuous EXP time-out events 
	private volatile long expCount=0;

	/*records the time interval between each probing pair
    compute the median packet pair interval of the last
	16 packet pair intervals (PI) and the estimate link capacity.(packet/s)*/
	private final PacketPairWindow packetPairWindow;

	//estimated link capacity
	long estimateLinkCapacity;
	// the packet arrival rate
	long packetArrivalSpeed;

	//round trip time, calculated from ACK/ACK2 pairs
	long roundTripTime=0;
	//round trip time variance
	long roundTripTimeVar=roundTripTime/2;

	//to check the ACK, NAK, or EXP timer
	private long nextACK;
	//microseconds to next ACK event
	private long ackTimerInterval=Util.getSYNTime();

	private long nextNAK;
	//microseconds to next NAK event
	private long nakTimerInterval=Util.getSYNTime();

	private long nextEXP;
	//microseconds to next EXP event
	private long expTimerInterval=100*Util.getSYNTime();

	//instant when the session was created (for expiry checking)
	private final long sessionUpSince;
	//milliseconds to timeout a new session that stays idle
	private final long IDLE_TIMEOUT = 3*60*1000;

	//buffer size for storing data
	private final long bufferSize;

	//stores received packets to be sent
	private final BlockingQueue<UDTPacket>handoffQueue;

	private Thread receiverThread;

	private volatile boolean stopped=false;

	//(optional) ack interval (see CongestionControl interface)
	private volatile long ackInterval=-1;

	/**
	 * if set to true connections will not expire, but will only be
	 * closed by a Shutdown message
	 */
	public static boolean connectionExpiryDisabled=false;

	private final boolean storeStatistics;
	
	/**
	 * create a receiver with a valid {@link UDTSession}
	 * @param session
	 */
	public UDTReceiver(UDTSession session,UDPEndPoint endpoint){
		this.endpoint = endpoint;
		this.session=session;
		this.sessionUpSince=System.currentTimeMillis();
		this.statistics=session.getStatistics();
		if(!session.isReady())throw new IllegalStateException("UDTSession is not ready.");
		ackHistoryWindow = new AckHistoryWindow(16);
		packetHistoryWindow = new PacketHistoryWindow(16);
		receiverLossList = new ReceiverLossList();
		packetPairWindow = new PacketPairWindow(16);
		largestReceivedSeqNumber=session.getInitialSequenceNumber()-1;
		bufferSize=session.getReceiveBufferSize();
		handoffQueue=new ArrayBlockingQueue<UDTPacket>(4*session.getFlowWindowSize());
		storeStatistics=Boolean.getBoolean("udt.receiver.storeStatistics");
		initMetrics();
		start();
	}
	
	private MeanValue dgReceiveInterval;
	private MeanValue dataPacketInterval;
	private MeanValue processTime;
	private MeanValue dataProcessTime;
	private void initMetrics(){
		if(!storeStatistics)return;
		dgReceiveInterval=new MeanValue("UDT receive interval");
		statistics.addMetric(dgReceiveInterval);
		dataPacketInterval=new MeanValue("Data packet interval");
		statistics.addMetric(dataPacketInterval);
		processTime=new MeanValue("UDT packet process time");
		statistics.addMetric(processTime);
		dataProcessTime=new MeanValue("Data packet process time");
		statistics.addMetric(dataProcessTime);
	}


	//starts the sender algorithm
	private void start(){
		Runnable r=new Runnable(){
			public void run(){
				try{
					nextACK=Util.getCurrentTime()+ackTimerInterval;
					nextNAK=(long)(Util.getCurrentTime()+1.5*nakTimerInterval);
					nextEXP=Util.getCurrentTime()+2*expTimerInterval;
					ackInterval=session.getCongestionControl().getAckInterval();
					while(!stopped){
						receiverAlgorithm();
					}
				}
				catch(Exception ex){
					logger.log(Level.SEVERE,"",ex);
				}
				logger.info("STOPPING RECEIVER for "+session);
			}
		};
		receiverThread=UDTThreadFactory.get().newThread(r);
		receiverThread.start();
	}

	/*
	 * packets are written by the endpoint
	 */
	protected void receive(UDTPacket p)throws IOException{
		if(storeStatistics)dgReceiveInterval.end();
		handoffQueue.offer(p);
		if(storeStatistics)dgReceiveInterval.begin();
	}

	/**
	 * receiver algorithm 
	 * see specification P11.
	 */
	public void receiverAlgorithm()throws InterruptedException,IOException{
		//check ACK timer
		long currentTime=Util.getCurrentTime();
		if(nextACK<currentTime){
			nextACK=currentTime+ackTimerInterval;
			processACKEvent(true);
		}
		//check NAK timer
		if(nextNAK<currentTime){
			nextNAK=currentTime+nakTimerInterval;
			processNAKEvent();
		}

		//check EXP timer
		if(nextEXP<currentTime){
			nextEXP=currentTime+expTimerInterval;
			processEXPEvent();
		}
		//perform time-bounded UDP receive
		UDTPacket packet=handoffQueue.poll(Util.getSYNTime(), TimeUnit.MICROSECONDS);
		if(packet!=null){
			//reset exp count to 1
			expCount=1;
			//If there is no unacknowledged data packet, or if this is an 
			//ACK or NAK control packet, reset the EXP timer.
			boolean needEXPReset=false;
			if(packet.isControlPacket()){
				ControlPacket cp=(ControlPacket)packet;
				int cpType=cp.getControlPacketType();
				if(cpType==ControlPacketType.ACK.ordinal() || cpType==ControlPacketType.NAK.ordinal()){
					needEXPReset=true;
				}
			}
			if(needEXPReset){
				nextEXP=Util.getCurrentTime()+expTimerInterval;
			}
			if(storeStatistics)processTime.begin();
			
			processUDTPacket(packet);
			
			if(storeStatistics)processTime.end();
		}
		
		Thread.yield();
	}

	/**
	 * process ACK event (see spec. p 12)
	 */
	protected void processACKEvent(boolean isTriggeredByTimer)throws IOException{
		//(1).Find the sequence number *prior to which* all the packets have been received
		final long ackNumber;
		ReceiverLossListEntry entry=receiverLossList.getFirstEntry();
		if (entry==null) {
			ackNumber = largestReceivedSeqNumber + 1;
		} else {
			ackNumber = entry.getSequenceNumber();
		}
		//(2).a) if ackNumber equals to the largest sequence number ever acknowledged by ACK2
		if (ackNumber == largestAcknowledgedAckNumber){
			//do not send this ACK
			return;
		}else if (ackNumber==lastAckNumber) {
			//or it is equals to the ackNumber in the last ACK  
			//and the time interval between these two ACK packets
			//is less than 2 RTTs,do not send(stop)
			long timeOfLastSentAck=ackHistoryWindow.getTime(lastAckNumber);
			if(Util.getCurrentTime()-timeOfLastSentAck< 2*roundTripTime){
				return;
			}
		}
		final long ackSeqNumber;
		//if this ACK is not triggered by ACK timers,send out a light Ack and stop.
		if(!isTriggeredByTimer){
			ackSeqNumber=sendLightAcknowledgment(ackNumber);
			return;
		}
		else{
			//pack the packet speed and link capacity into the ACK packet and send it out.
			//(7).records  the ACK number,ackseqNumber and the departure time of
			//this Ack in the ACK History Window
			ackSeqNumber=sendAcknowledgment(ackNumber);
		}
		AckHistoryEntry sentAckNumber= new AckHistoryEntry(ackSeqNumber,ackNumber,Util.getCurrentTime());
		ackHistoryWindow.add(sentAckNumber);
		//store ack number for next iteration
		lastAckNumber=ackNumber;
	}

	/**
	 * process NAK event (see spec. p 13)
	 */
	protected void processNAKEvent()throws IOException{
		//find out all sequence numbers whose last feedback time larger than is k*RTT
		List<Long>seqNumbers=receiverLossList.getFilteredSequenceNumbers(roundTripTime,true);
		sendNAK(seqNumbers);
	}

	/**
	 * process EXP event (see spec. p 13)
	 */
	protected void processEXPEvent()throws IOException{
		if(session.getSocket()==null)return;
		UDTSender sender=session.getSocket().getSender();
		//put all the unacknowledged packets in the senders loss list
		sender.putUnacknowledgedPacketsIntoLossList();
		if(expCount>16 && System.currentTimeMillis()-sessionUpSince > IDLE_TIMEOUT){
			if(!connectionExpiryDisabled &&!stopped){
				sendShutdown();
				stop();
				logger.info("Session "+session+" expired.");
				return;
			}
		}
		if(!sender.haveLostPackets()){
			sendKeepAlive();
		}
		expCount++;
	}

	protected void processUDTPacket(UDTPacket p)throws IOException{
		//(3).Check the packet type and process it according to this.
		
		if(!p.isControlPacket()){
			DataPacket dp=(DataPacket)p;
			if(storeStatistics){
				dataPacketInterval.end();
				dataProcessTime.begin();
			}
			onDataPacketReceived(dp);
			if(storeStatistics){
				dataProcessTime.end();
				dataPacketInterval.begin();
			}
		}

		else if (p.getControlPacketType()==ControlPacketType.ACK2.ordinal()){
			Acknowledgment2 ack2=(Acknowledgment2)p;
			onAck2PacketReceived(ack2);
		}

		else if (p instanceof Shutdown){
			onShutdown();
		}

	}

	//every nth packet will be discarded... for testing only of course
	public static int dropRate=0;
	
	//number of received data packets
	private int n=0;
	
	protected void onDataPacketReceived(DataPacket dp)throws IOException{
		long currentSequenceNumber = dp.getPacketSequenceNumber();
		
		//for TESTING : check whether to drop this packet
//		n++;
//		//if(dropRate>0 && n % dropRate == 0){
//			if(n % 1111 == 0){	
//				logger.info("**** TESTING:::: DROPPING PACKET "+currentSequenceNumber+" FOR TESTING");
//				return;
//			}
//		//}
		boolean OK=session.getSocket().getInputStream().haveNewData(currentSequenceNumber,dp.getData());
		if(!OK){
			//need to drop packet...
			return;
		}
		
		long currentDataPacketArrivalTime = Util.getCurrentTime();

		/*(4).if the seqNo of the current data packet is 16n+1,record the
		time interval between this packet and the last data packet
		in the packet pair window*/
		if((currentSequenceNumber%16)==1 && lastDataPacketArrivalTime>0){
			long interval=currentDataPacketArrivalTime -lastDataPacketArrivalTime;
			packetPairWindow.add(interval);
		}
		
		//(5).record the packet arrival time in the PKT History Window.
		packetHistoryWindow.add(currentDataPacketArrivalTime);

		
		//store current time
		lastDataPacketArrivalTime=currentDataPacketArrivalTime;

		
		//(6).number of detected lossed packet
		/*(6.a).if the number of the current data packet is greater than LSRN+1,
			put all the sequence numbers between (but excluding) these two values
			into the receiver's loss list and send them to the sender in an NAK packet*/
		if(SequenceNumber.compare(currentSequenceNumber,largestReceivedSeqNumber+1)>0){
			sendNAK(currentSequenceNumber);
		}
		else if(SequenceNumber.compare(currentSequenceNumber,largestReceivedSeqNumber)<0){
				/*(6.b).if the sequence number is less than LRSN,remove it from
				 * the receiver's loss list
				 */
				receiverLossList.remove(currentSequenceNumber);
		}

		statistics.incNumberOfReceivedDataPackets();

		//(7).Update the LRSN
		if(SequenceNumber.compare(currentSequenceNumber,largestReceivedSeqNumber)>0){
			largestReceivedSeqNumber=currentSequenceNumber;
		}

		//(8) need to send an ACK? Some cc algorithms use this
		if(ackInterval>0){
			if(n % ackInterval == 0)processACKEvent(false);
		}
	}

	/**
	 * write a NAK triggered by a received sequence number that is larger than
	 * the largestReceivedSeqNumber + 1
	 * @param currentSequenceNumber - the currently received sequence number
	 * @throws IOException
	 */
	protected void sendNAK(long currentSequenceNumber)throws IOException{
		NegativeAcknowledgement nAckPacket= new NegativeAcknowledgement();
		nAckPacket.addLossInfo(largestReceivedSeqNumber+1, currentSequenceNumber);
		nAckPacket.setSession(session);
		nAckPacket.setDestinationID(session.getDestination().getSocketID());
		//put all the sequence numbers between (but excluding) these two values into the
		//receiver loss list
		for(long i=largestReceivedSeqNumber+1;i<currentSequenceNumber;i++){
			ReceiverLossListEntry detectedLossSeqNumber= new ReceiverLossListEntry(i);
			receiverLossList.insert(detectedLossSeqNumber);
		}
		endpoint.doSend(nAckPacket);
		//logger.info("NAK for "+currentSequenceNumber);
		statistics.incNumberOfNAKSent();
	}

	protected void sendNAK(List<Long>sequenceNumbers)throws IOException{
		if(sequenceNumbers.size()==0)return;
		NegativeAcknowledgement nAckPacket= new NegativeAcknowledgement();
		nAckPacket.addLossInfo(sequenceNumbers);
		nAckPacket.setSession(session);
		nAckPacket.setDestinationID(session.getDestination().getSocketID());
		endpoint.doSend(nAckPacket);
		statistics.incNumberOfNAKSent();
	}

	protected long sendLightAcknowledgment(long ackNumber)throws IOException{
		Acknowledgement acknowledgmentPkt=buildLightAcknowledgement(ackNumber);
		endpoint.doSend(acknowledgmentPkt);
		statistics.incNumberOfACKSent();
		return acknowledgmentPkt.getAckSequenceNumber();
	}

	protected long sendAcknowledgment(long ackNumber)throws IOException{
		Acknowledgement acknowledgmentPkt = buildLightAcknowledgement(ackNumber);
		//set the estimate link capacity
		estimateLinkCapacity=packetPairWindow.getEstimatedLinkCapacity();
		acknowledgmentPkt.setEstimatedLinkCapacity(estimateLinkCapacity);
		//set the packet arrival rate
		packetArrivalSpeed=packetHistoryWindow.getPacketArrivalSpeed();
		acknowledgmentPkt.setPacketReceiveRate(packetArrivalSpeed);

		endpoint.doSend(acknowledgmentPkt);

		statistics.incNumberOfACKSent();
		statistics.setPacketArrivalRate(packetArrivalSpeed, estimateLinkCapacity);
		return acknowledgmentPkt.getAckSequenceNumber();
	}

	//builds a "light" Acknowledgement
	private Acknowledgement buildLightAcknowledgement(long ackNumber){
		Acknowledgement acknowledgmentPkt = new Acknowledgement();
		//the packet sequence number to which all the packets have been received
		acknowledgmentPkt.setAckNumber(ackNumber);
		//assign this ack a unique increasing ACK sequence number
		acknowledgmentPkt.setAckSequenceNumber(++ackSequenceNumber);
		acknowledgmentPkt.setRoundTripTime(roundTripTime);
		acknowledgmentPkt.setRoundTripTimeVar(roundTripTimeVar);
		//set the buffer size
		acknowledgmentPkt.setBufferSize(bufferSize);

		acknowledgmentPkt.setDestinationID(session.getDestination().getSocketID());
		acknowledgmentPkt.setSession(session);

		return acknowledgmentPkt;
	}

	/**
	 * spec p. 13: <br/>
	  1) Locate the related ACK in the ACK History Window according to the 
         ACK sequence number in this ACK2.  <br/>
      2) Update the largest ACK number ever been acknowledged. <br/>
      3) Calculate new rtt according to the ACK2 arrival time and the ACK 
         departure time, and update the RTT value as: RTT = (RTT * 7 + 
         rtt) / 8.  <br/>
      4) Update RTTVar by: RTTVar = (RTTVar * 3 + abs(RTT - rtt)) / 4.  <br/>
      5) Update both ACK and NAK period to 4 * RTT + RTTVar + SYN.  <br/>
	 */
	protected void onAck2PacketReceived(Acknowledgment2 ack2){
		AckHistoryEntry entry=ackHistoryWindow.getEntry(ack2.getAckSequenceNumber());
		if(entry!=null){
			long ackNumber=entry.getAckNumber();
			largestAcknowledgedAckNumber=Math.max(ackNumber, largestAcknowledgedAckNumber);
			
			long rtt=entry.getAge();
			if(roundTripTime>0)roundTripTime = (roundTripTime*7 + rtt)/8;
			else roundTripTime = rtt;
			roundTripTimeVar = (roundTripTimeVar* 3 + Math.abs(roundTripTimeVar- rtt)) / 4;
			ackTimerInterval=4*roundTripTime+roundTripTimeVar+Util.getSYNTime();
			nakTimerInterval=ackTimerInterval;
			statistics.setRTT(roundTripTime, roundTripTimeVar);
		}
	}

	protected void sendKeepAlive()throws IOException{
		KeepAlive ka=new KeepAlive();
		ka.setDestinationID(session.getDestination().getSocketID());
		ka.setSession(session);
		endpoint.doSend(ka);
	}

	protected void sendShutdown()throws IOException{
		Shutdown s=new Shutdown();
		s.setDestinationID(session.getDestination().getSocketID());
		s.setSession(session);
		endpoint.doSend(s);
	}

	private volatile long ackSequenceNumber=0;

	protected void resetEXPTimer(){
		nextEXP=Util.getCurrentTime()+expTimerInterval;
		expCount=0;
	}

	protected void resetEXPCount(){
		expCount=0;
	}
	
	public void setAckInterval(long ackInterval){
		this.ackInterval=ackInterval;
	}
	
	protected void onShutdown()throws IOException{
		stop();
	}

	public void stop()throws IOException{
		stopped=true;
		session.getSocket().close();
		//stop our sender as well
		session.getSocket().getSender().stop();
	}

	public String toString(){
		StringBuilder sb=new StringBuilder();
		sb.append("UDTReceiver ").append(session).append("\n");
		sb.append("LossList: "+receiverLossList);
		return sb.toString();
	}

}
