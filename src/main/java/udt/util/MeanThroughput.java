package udt.util;


/**
 * holds a floating mean value
 */
public class MeanThroughput extends MeanValue{
	
	private final double packetSize;
	
	public MeanThroughput(String name, int packetSize){
		this(name, false, 64, packetSize);
	}
	
	public MeanThroughput(String name, boolean verbose, int packetSize){
		this(name, verbose, 64, packetSize);
	}
	
	public MeanThroughput(String name, boolean verbose, int nValue, int packetSize){
		super(name,verbose,nValue);
		this.packetSize=packetSize;
	}

	@Override
	public double getMean() {
		return packetSize/super.getMean();
	}
	

}
