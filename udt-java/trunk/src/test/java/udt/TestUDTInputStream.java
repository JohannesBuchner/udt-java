package udt;

import java.security.MessageDigest;

import udt.util.UDTStatistics;
import udt.util.Util;

public class TestUDTInputStream extends UDTTestBase{

	public void test1()throws Exception{
		UDTStatistics stat=new UDTStatistics("test");
		UDTInputStream is=new UDTInputStream(null, stat);
		byte[] data1="this is ".getBytes();
		byte[] data2="a test".getBytes();
		byte[] data3=" string".getBytes();
		String digest=computeMD5(data1,data2,data3);
		is.haveNewData(1, data1);
		is.haveNewData(2, data2);
		is.haveNewData(3, data3);
		is.noMoreData();
		is.setBlocking(false);
		String readMD5=readAll(is,8);
		assertEquals(digest,readMD5);
	}
	
	public void test2()throws Exception{
		UDTStatistics stat=new UDTStatistics("test");
		UDTInputStream is=new UDTInputStream(null, stat);
		byte[] data1=getRandomData(65537);
		byte[] data2=getRandomData(1234);
		byte[] data3=getRandomData(3*1024*1024);
		String digest=computeMD5(data1,data2,data3);
		is.setBlocking(false);
		is.haveNewData(1, data1);
		is.haveNewData(2, data2);
		is.haveNewData(3, data3);
		is.noMoreData();
		String readMD5=readAll(is,5*1024*1024);
		assertEquals(digest,readMD5);
	}
	
	public void testInOrder()throws Exception{
		UDTStatistics stat=new UDTStatistics("test");
		UDTInputStream is=new UDTInputStream(null, stat);
		is.setBlocking(false);
		byte[]data=getRandomData(10*1024*1024);
		
		byte[][]blocks=makeChunks(10,data);
		String digest=computeMD5(blocks);
		
		for(int i=0;i<10;i++){
			is.haveNewData(i+1, blocks[i]);
		}
		is.noMoreData();
		
		String readMD5 = readAll(is,1024*999);
		assertEquals(digest,readMD5);
	}
	
	public void testRandomOrder()throws Exception{
		UDTStatistics stat=new UDTStatistics("test");
		UDTInputStream is=new UDTInputStream(null, stat);
		is.setBlocking(false);
		byte[]data=getRandomData(100*1024);
		
		byte[][]blocks=makeChunks(10,data);
		String digest=computeMD5(blocks);
		
		byte[]order=new byte[]{9,7,5,3,1,2,0,4,6,8};
		
		for(int i : order){
			is.haveNewData(i+1, blocks[i]);
		}
		String readMD5=readAll(is,512,true);
		
		assertEquals(digest,readMD5);
	}
	
	//read and discard data from the given input stream
	//returns the md5 digest of the data
	protected String readAll(UDTInputStream is, int bufsize,boolean sendNoMoreData)throws Exception{
		MessageDigest d=MessageDigest.getInstance("MD5");
		int c=0;
		byte[]buf=new byte[bufsize];
		while(true){
			c=is.read(buf);
			is.noMoreData();
			if(c==-1)break;
			else{
				if(c>0)d.update(buf,0,c);
			}
		}
		return Util.hexString(d);
	}
	
}

