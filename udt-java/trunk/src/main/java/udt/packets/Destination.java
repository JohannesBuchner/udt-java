package udt.packets;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class Destination {

	private String host;
	private int port;
	private InetAddress address;
	
	public Destination(String host, int port){
		this.host=host;
		this.port=port;
	}
	
	public Destination(InetAddress address, int port){
		this.address=address;
		this.port=port;
	}
	
	public InetAddress getAddress()throws UnknownHostException{
		if(address!=null)return address;
		
		return InetAddress.getByName(host);
	}
	
	public int getPort(){
		return port;
	}
	
	public String toString(){
		if(address!=null)return("Destination: "+address.getHostName()+" port="+port);
		return "Destination host="+host+" port="+port;
	}
}
