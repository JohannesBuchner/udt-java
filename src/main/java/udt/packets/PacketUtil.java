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


public class PacketUtil {

	public static byte[]encode(long value){
		byte m4= (byte) (value>>24 );
		byte m3=(byte)(value>>16);
		byte m2=(byte)(value>>8);
		byte m1=(byte)(value);
		return new byte[]{m4,m3,m2,m1};
	}
	
	public static byte[]encodeSetHighest(boolean highest,long value){
		byte m4;
		if(highest){
			m4= (byte) (0x80 | value>>24 );
		}
		else{
			m4= (byte) (0x7f & value>>24 );
		}
		byte m3=(byte)(value>>16);
		byte m2=(byte)(value>>8);
		byte m1=(byte)(value);
		return new byte[]{m4,m3,m2,m1};
	}
	

	public static byte[]encodeControlPacketType(int type){
		byte m4=(byte) 0x80;
		
		byte m3=(byte)type;
		return new byte[]{m4,m3,0,0};
	}
	
	
	public static long decode(byte[]data, int start){
		long result = (data[start]&0xFF)<<24
		             | (data[start+1]&0xFF)<<16
					 | (data[start+2]&0xFF)<<8
					 | (data[start+3]&0xFF);
		return result;
	}
	
	
	public static int decodeType(byte[]data, int start){
		int result =  data[start+1]&0xFF;
		return result;
	}

}
