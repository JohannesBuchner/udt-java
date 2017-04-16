package udt.util;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * holds a floating mean timing value (measured in microseconds)
 */
public class MeanValue {

	private double mean=0;
	private double max=0;
	private double min=0;
	
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
		max=Math.max(max, value);
		min=Math.min(max, value);
		
		if(verbose &&  n % nValue == 0){
			if(msg!=null)System.out.println(msg+" "+get());
			else System.out.println(name+" "+get());
			
			max=0;
			min=0;
		}
	}
	
	public double getMean(){
		return mean;
	}
	
	public String getFormattedMean(){
		return format.format(getMean());
	}
	
	public String get(){
		return format.format(getMean())+" max="+format.format(max)+" min="+format.format(min);
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
