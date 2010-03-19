package udt;

public interface UDTPacket {


	public long getMessageNumber();
	public void setMessageNumber(long messageNumber) ;
	
	
	public void setTimeStamp(long timeStamp);
	public long getTimeStamp();
	

	public void setDestinationID(long destinationID);
	public long getDestinationID();
	
	public boolean isControlPacket();
	
	/**
	 * header
	 * @return
	 */

	public byte[] getHeader();

	public byte[] getEncoded();
	
	/**
	 * return <code>true</code> if this packet should be routed to
	 * the {@link UDTSender} 
	 * @return
	 */
	public boolean forSender();
}
