package udt.util;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class Application implements Runnable {
	
	protected static boolean verbose;

	protected static String localIP=null;

	protected static int localPort=-1;

	public void configure(){
		if(verbose){
			Logger.getLogger("udt").setLevel(Level.INFO);
		}
		else{
			Logger.getLogger("udt").setLevel(Level.OFF);
		}
	}
	
	
	protected static String[] parseOptions(String[] args){
		List<String>newArgs=new ArrayList<String>();
		for(String arg: args){
			if(arg.startsWith("-")){
				parseArg(arg);
			}
			else
			{
				newArgs.add(arg);
			}
		}
		return newArgs.toArray(new String[newArgs.size()]);
	}
	
	
	protected static void parseArg(String arg){
		if("-v".equals(arg) || "--verbose".equals(arg)){
			verbose=true;
			return;
		}
		if(arg.startsWith("--localIP")){
			localIP=arg.split("=")[1];
		}
		if(arg.startsWith("--localPort")){
			localPort=Integer.parseInt(arg.split("=")[1]);
		}
	}
	
	
	

	static long decode(byte[]data, int start){
		long result = (data[start+3] & 0xFF)<<24
		             |(data[start+2] & 0xFF)<<16
					 |(data[start+1] & 0xFF)<<8
					 |(data[start] & 0xFF);
		return result;
	}
	
	static byte[]encode(long value){
		byte m4= (byte) (value>>24 );
		byte m3=(byte)(value>>16);
		byte m2=(byte)(value>>8);
		byte m1=(byte)(value);
		return new byte[]{m1,m2,m3,m4};
	}
	
	static byte[]encode64(long value){
		byte m4= (byte) (value>>24 );
		byte m3=(byte)(value>>16);
		byte m2=(byte)(value>>8);
		byte m1=(byte)(value);
		return new byte[]{m1,m2,m3,m4,0,0,0,0};
	}
}
