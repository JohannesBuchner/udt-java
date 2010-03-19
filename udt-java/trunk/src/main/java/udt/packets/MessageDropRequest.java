package udt.packets;
import java.io.ByteArrayOutputStream;


public class MessageDropRequest extends ControlPacket{
	//Bits 35-64: Message number
	
	private long msgFirstSeqNo;
	private long msgLastSeqNo;
	
	public MessageDropRequest(){
		this.contrlPktTyp=ControlPacketType.MESSAGE_DROP_REQUEST.ordinal();
	}
	
	public MessageDropRequest(byte[]controlInformation){
		this();
		//this.controlInformation=controlInformation;
		decode(controlInformation );
	}
	void decode(byte[]data){
		msgFirstSeqNo =PacketUtil.decode(data, 0);
		msgLastSeqNo =PacketUtil.decode(data, 4);
	}

	public long getMsgFirstSeqNo() {
		return msgFirstSeqNo;
	}

	public void setMsgFirstSeqNo(long msgFirstSeqNo) {
		this.msgFirstSeqNo = msgFirstSeqNo;
	}

	public long getMsgLastSeqNo() {
		return msgLastSeqNo;
	}

	public void setMsgLastSeqNo(long msgLastSeqNo) {
		this.msgLastSeqNo = msgLastSeqNo;
	}

	@Override
	public byte[] encodeControlInformation() {
		try {
			ByteArrayOutputStream bos=new ByteArrayOutputStream();
			bos.write(PacketUtil.encode(msgFirstSeqNo));
			bos.write(PacketUtil.encode(msgLastSeqNo));
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
		MessageDropRequest other = (MessageDropRequest) obj;
		if (msgFirstSeqNo != other.msgFirstSeqNo)
			return false;
		if (msgLastSeqNo != other.msgLastSeqNo)
			return false;
		return true;
	}
	 
	

}
