package udt.util;

import java.text.NumberFormat;

/**
 * holds a floating mean value
 */
public class MeanValue {

	private double mean=0;
	
	private int n=0;
	
	private final NumberFormat format;
	
	
	public MeanValue(){
		format=NumberFormat.getNumberInstance();
		format.setMaximumFractionDigits(2);
	}
	public void addValue(double value){
		mean=(mean*n+value)/(n+1);
		n++;
	}
	
	public double getMean(){
		return mean;
	}
	
	public String getFormattedMean(){
		return format.format(mean);
	}
	
	public void clear(){
		mean=0;
		n=0;
	}
}
