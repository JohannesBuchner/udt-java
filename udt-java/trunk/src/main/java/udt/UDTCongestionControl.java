package udt;

import java.util.List;

public class UDTCongestionControl {

	private final UDTSession session;
	
	private int sendInterval=0;
	
	public UDTCongestionControl(UDTSession session){
		this.session=session;
	}

	public void onNAK(List<Integer>lossInfo){
		
	}

	public int getSendInterval(){
		return sendInterval;
	}
	
}
