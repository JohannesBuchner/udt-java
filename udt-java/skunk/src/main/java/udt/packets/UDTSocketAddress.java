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

import java.net.InetAddress;
import java.net.SocketAddress;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

public class UDTSocketAddress extends SocketAddress {
    private static final long serialVersionUID = 1L;

	private final int port;

	private final InetAddress address;
	
	//UDT socket ID of the peer
	private int socketID;
	
        /**
         * Wild card InetAddress or an ephemeral port of 0 may be used.
         * A socketID of 0 is also considered ephemeral, in that the 
         * implementation will assign the next available socketID.
         * 
         * For this reason, the UDTSocket should always be asked for
         * its SocketAddress, it should never be assumed that it hasn't changed.
         * 
         * @param address
         * @param port
         * @param socketID 
         */
	public UDTSocketAddress(InetAddress address, int port, int socketID){
		this.address=address;
		this.port=port;
                this.socketID=socketID;
	}
	
	public InetAddress getAddress(){
		return address;
	}
	
	public int getPort(){
		return port;
	}
	
	public int getSocketID() {
		return socketID;
	}

	public void setSocketID(int socketID) {
		this.socketID = socketID;
	}

	public String toString(){
		return("UDTSocketAddress ["+address.getHostName()+" port="+port+" socketID="+socketID)+"]";
	} 

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((address == null) ? 0 : address.hashCode());
		result = prime * result + port;
		result = prime * result + (int) (socketID ^ (socketID >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		UDTSocketAddress other = (UDTSocketAddress) obj;
		if (address == null) {
			if (other.address != null)
				return false;
		} else if (!address.equals(other.address))
			return false;
		if (port != other.port)
			return false;
		if (socketID != other.socketID)
			return false;
		return true;
	}
	
	
}
