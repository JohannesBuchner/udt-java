package udt.performance;

import java.util.logging.Level;
import java.util.logging.Logger;

import udt.UDTReceiver;
import udt.UDTSession;
import udt.cc.SimpleTCP;

//uses different CC algorithm
public class TestUDTLargeDataCC1 extends TestUDTLargeData{
	
	boolean running=false;

	//how many
	int num_packets=100;
	
	//how large is a single packet
	int size=1*1024*1024;
	
	int TIMEOUT=Integer.MAX_VALUE;
	
	int READ_BUFFERSIZE=1*1024*1024;

	public void test1()throws Exception{
		Logger.getLogger("udt").setLevel(Level.INFO);
		UDTReceiver.dropRate=0;
		System.setProperty(UDTSession.CC_CLASS, SimpleTCP.class.getName());
		TIMEOUT=Integer.MAX_VALUE;
		doTest();
	}

}
