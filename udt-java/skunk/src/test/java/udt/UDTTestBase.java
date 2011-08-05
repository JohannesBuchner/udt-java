package udt;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.Random;

import udt.util.Util;

import junit.framework.TestCase;

/**
 * some additional utilities useful for testing
 */
public abstract class UDTTestBase extends TestCase{
	
	//get an array filled with random data
	protected byte[] getRandomData(int size){
		byte[]data=new byte[size];
		new Random().nextBytes(data);
		return data;
	}
	
	//compute the md5 hash
	protected String computeMD5(byte[]...datablocks)throws Exception{
		MessageDigest d=MessageDigest.getInstance("MD5");
		for(byte[]data: datablocks){
			d.update(data);
		}
		return hexString(d);
	}
	
	//read and discard data from the given input stream
	//returns the md5 digest of the data
	protected String readAll(InputStream is, int bufsize)throws Exception{
		MessageDigest d=MessageDigest.getInstance("MD5");
		int c=0;
		byte[]buf=new byte[bufsize];
		while(true){
			c=is.read(buf);
			if(c==-1)break;
			else{
				d.update(buf,0,c);
			}
		}
		return hexString(d);
	}
	
	protected byte[][]makeChunks(int number, byte[] data){
		int chunksize=data.length/number;
		byte[][]result=new byte[number][chunksize];
		ByteBuffer bb=ByteBuffer.wrap(data);
		int i=0;
		while(bb.remaining()>0){
			int len=Math.min(chunksize, bb.remaining());
			bb.get(result[i],0,len);
			i++;
		}
		return result;
	}

	public static String hexString(MessageDigest digest){
		return Util.hexString(digest);
	}

	
}
