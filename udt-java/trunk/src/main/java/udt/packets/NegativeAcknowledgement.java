package udt.packets;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * NAK carries information about lost packets
 * 
 * loss info is described in the spec on p.15
 */
public class NegativeAcknowledgement extends ControlPacket{

	//after decoding this contains the lost sequence numbers
	List<Integer>lostSequenceNumbers;

	//this contains the loss information intervals as described on p.15 of the spec
	ByteArrayOutputStream lossInfo=new ByteArrayOutputStream();

	public NegativeAcknowledgement(){
		this.contrlPktTyp=ControlPacketType.NAK.ordinal();
	}

	public NegativeAcknowledgement(byte[]controlInformation){
		this();
		lostSequenceNumbers=decode(controlInformation);
	}

	/**
	 * decode the loss info
	 * @param lossInfo
	 */
	private List<Integer> decode(byte[]lossInfo){
		List<Integer>lostSequenceNumbers=new ArrayList<Integer>();
		ByteBuffer bb=ByteBuffer.wrap(lossInfo);
		byte[]buffer=new byte[4];
		while(bb.remaining()>0){
			//read 4 bytes
			buffer[0]=bb.get();
			buffer[1]=bb.get();
			buffer[2]=bb.get();
			buffer[3]=bb.get();
			boolean isNotSingle=(buffer[0]&128)!=0;
			//set highest bit back to 0
			buffer[0]=(byte)(buffer[0]&0x7f);
			int lost=ByteBuffer.wrap(buffer).getInt();
			if(isNotSingle){
				//get the end of the interval
				int end=bb.getInt();
				//and add all lost numbers to the result list
				for(int i=lost;i<=end;i++){
					lostSequenceNumbers.add(i);
				}
			}else{
				lostSequenceNumbers.add(lost);
			}
		}
		return lostSequenceNumbers;
	}

	/**
	 * add a single lost packet number
	 * @param singleSequenceNumber
	 */
	public void addLossInfo(long singleSequenceNumber) {
		byte[] enc=PacketUtil.encodeSetHighest(false, singleSequenceNumber);
		try{
			lossInfo.write(enc);
		}catch(IOException ignore){}
	}

	/**
	 * add an interval of lost packet numbers
	 * @param firstSequenceNumber
	 * @param lastSequenceNumber
	 */
	public void addLossInfo(long firstSequenceNumber, long lastSequenceNumber) {
		//check if we really need an interval
		if(lastSequenceNumber-firstSequenceNumber==1){
			addLossInfo(firstSequenceNumber);
			return;
		}
		//else add an interval
		byte[] enc1=PacketUtil.encodeSetHighest(true, firstSequenceNumber);
		byte[] enc2=PacketUtil.encodeSetHighest(false, lastSequenceNumber);
		try{
			lossInfo.write(enc1);
			lossInfo.write(enc2);
		}catch(IOException ignore){}
	}

	/**
	 * pack the given list of sequence numbers and add them to the loss info
	 * @param sequenceNumbers - a list of sequence numbers
	 */
	public void addLossInfo(List<Long>sequenceNumbers) {
		long start=0;
		int index=0;
		do{
			start=sequenceNumbers.get(index);
			long end=0;
			int c=0;
			do{
				c++;
				index++;
				if(index<sequenceNumbers.size()){
					end=sequenceNumbers.get(index);
				}
			}while(end-start==c);
			if(end==0){
				addLossInfo(start);
			}
			else{
				end=sequenceNumbers.get(index-1);
				addLossInfo(start, end);
			}
		}while(index<sequenceNumbers.size());
	}

	/**
	 * Return the lost packet numbers
	 * @return
	 */
	public List<Integer> getDecodedLossInfo() {
		return lostSequenceNumbers;
	}

	@Override
	public byte[] encodeControlInformation(){
		try {
			return lossInfo.toByteArray();
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
		NegativeAcknowledgement other = (NegativeAcknowledgement) obj;
		
		List<Integer>thisLost=null;
		List<Integer>otherLost=null;
		
		//compare the loss info
		if(lostSequenceNumbers!=null){
			thisLost=lostSequenceNumbers;
		}else{
			thisLost=decode(lossInfo.toByteArray());
		}
		if(other.lostSequenceNumbers!=null){
			otherLost=other.lostSequenceNumbers;
		}else{
			otherLost=other.decode(other.lossInfo.toByteArray());
		}
		if(!thisLost.equals(otherLost)){
			return false;
		}

		return true;
	}


}
