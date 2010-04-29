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

package udt.packets;

import java.io.ByteArrayOutputStream;

public class ConnectionHandshake extends ControlPacket {
	private long udtVersion=4;
	
	public static final long SOCKET_TYPE_STREAM=0;
	public static final long SOCKET_TYPE_DGRAM=1;
	private long socketType= SOCKET_TYPE_STREAM;//STREAM OR DGRAM
	
	private long initialSeqNo = 0;
	private long packetSize;
	private long maxFlowWndSize;
	
	public static final long CONNECTION_TYPE_REGULAR=0;
	public static final long CONNECTION_TYPE_RENDEZVOUS=1;
	private long connectionType = 0;//regular or rendezvous mode
	
	private long socketID;
	
	public ConnectionHandshake(){
		this.controlPacketType=ControlPacketType.CONNECTION_HANDSHAKE.ordinal();
	}
	
	public ConnectionHandshake(byte[]controlInformation){
		this();
		//this.controlInformation=controlInformation;
		decode(controlInformation);
	}
	
	//faster than instanceof...
	public boolean isConnectionHandshake(){
		return true;
	}
	
	void decode(byte[]data){
		udtVersion =PacketUtil.decode(data, 0);
		socketType=PacketUtil.decode(data, 4);
		initialSeqNo=PacketUtil.decode(data, 8);
		packetSize=PacketUtil.decode(data, 12);
		maxFlowWndSize=PacketUtil.decode(data, 16);
		connectionType=PacketUtil.decode(data, 20);
		socketID=PacketUtil.decode(data, 24);
	}

	public long getUdtVersion() {
		return udtVersion;
	}
	public void setUdtVersion(long udtVersion) {
		this.udtVersion = udtVersion;
	}
	
	public long getSocketType() {
		return socketType;
	}
	public void setSocketType(long socketType) {
		this.socketType = socketType;
	}
	
	public long getInitialSeqNo() {
		return initialSeqNo;
	}
	public void setInitialSeqNo(long initialSeqNo) {
		this.initialSeqNo = initialSeqNo;
	}
	
	public long getPacketSize() {
		return packetSize;
	}
	public void setPacketSize(long packetSize) {
		this.packetSize = packetSize;
	}
	
	public long getMaxFlowWndSize() {
		return maxFlowWndSize;
	}
	public void setMaxFlowWndSize(long maxFlowWndSize) {
		this.maxFlowWndSize = maxFlowWndSize;
	}
	
	public long getConnectionType() {
		return connectionType;
	}
	public void setConnectionType(long connectionType) {
		this.connectionType = connectionType;
	}
	
	public long getSocketID() {
		return socketID;
	}
	public void setSocketID(long socketID) {
		this.socketID = socketID;
	}
	
	@Override
	public byte[] encodeControlInformation(){
		try {
			ByteArrayOutputStream bos=new ByteArrayOutputStream(24);
			bos.write(PacketUtil.encode(udtVersion));
			bos.write(PacketUtil.encode(socketType));
			bos.write(PacketUtil.encode(initialSeqNo));
			bos.write(PacketUtil.encode(packetSize));
			bos.write(PacketUtil.encode(maxFlowWndSize));
			bos.write(PacketUtil.encode(connectionType));
			bos.write(PacketUtil.encode(socketID));
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
		ConnectionHandshake other = (ConnectionHandshake) obj;
		if (connectionType != other.connectionType)
			return false;
		if (initialSeqNo != other.initialSeqNo)
			return false;
		if (maxFlowWndSize != other.maxFlowWndSize)
			return false;
		if (packetSize != other.packetSize)
			return false;
		if (socketID != other.socketID)
			return false;
		if (socketType != other.socketType)
			return false;
		if (udtVersion != other.udtVersion)
			return false;
		return true;
	}
	
	
	

}
