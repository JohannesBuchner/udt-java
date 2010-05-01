package udt.util;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * holds a floating mean value
 */
public class MeanValue {

	private double mean=0;
	
	private int n=0;
	
	private final NumberFormat format;
	
	private final boolean verbose;
	
	private final long nValue; 
	private long start;
	
	private String msg;
	
	private final String name;
	
	public MeanValue(String name){
		this(name, false, 64);
	}
	
	public MeanValue(String name, boolean verbose){
		this(name, verbose, 64);
	}
	
	public MeanValue(String name, boolean verbose, int nValue){
		format=NumberFormat.getNumberInstance(Locale.ENGLISH);
		format.setMaximumFractionDigits(2);
		format.setGroupingUsed(false);
		this.verbose=verbose;
		this.nValue=nValue;
		this.name=name;
	}
	
	public void addValue(double value){
		mean=(mean*n+value)/(n+1);
		n++;
		if(verbose &&  n % nValue == 0){
			if(msg!=null)System.out.println(msg+" "+getFormattedMean());
			else System.out.println(getFormattedMean());
		}
	}
	
	public double getMean(){
		return mean;
	}
	
	public String getFormattedMean(){
		return format.format(getMean());
	}
	
	public void clear(){
		mean=0;
		n=0;
	}
	
	public void begin(){
		start=Util.getCurrentTime();
	}
	
	public void end(){
		if(start>0)addValue(Util.getCurrentTime()-start);
	}
	public void end(String msg){
		this.msg=msg;
		addValue(Util.getCurrentTime()-start);
	}
	
	public String getName(){
		return name;
	}
	
	public String toString(){
		return name;
	}
}
