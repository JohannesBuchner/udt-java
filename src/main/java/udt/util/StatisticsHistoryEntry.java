package udt.util;

import java.util.List;

public class StatisticsHistoryEntry {

	private final Object[] values;

	private final long timestamp;

	private final boolean isHeading;
	
	public StatisticsHistoryEntry(boolean heading, long time, Object ...values){
		this.values=values;
		this.isHeading=heading;
		this.timestamp=time;
	}

	public StatisticsHistoryEntry(boolean heading, long time, List<MeanValue>metrics){
		this.isHeading=heading;
		this.timestamp=time;
		int length=metrics.size();
		if(isHeading)length++;
		Object[]metricValues=new Object[length];
		if(isHeading){
			metricValues[0]="time";
			for(int i=0;i<metrics.size();i++){
				metricValues[i+1]=metrics.get(i).getName();
			}
		}
		else{
			for(int i=0;i<metricValues.length;i++){
				metricValues[i]=metrics.get(i).getFormattedMean();
			}
		}
		this.values=metricValues;
	}

	public StatisticsHistoryEntry(Object ...values){
		this(false,System.currentTimeMillis(),values);
	}

	/**
	 * output as comma separated list
	 */
	public String toString(){
		StringBuilder sb=new StringBuilder();
		if(!isHeading){
			sb.append(timestamp);
			for(Object val: values){
				sb.append(" , ").append(String.valueOf(val));
			}
		}
		else{
			for(int i=0;i<values.length;i++){
				if(i>0)sb.append(" , ");
				sb.append(String.valueOf(values[i]));
			}
		}
		return sb.toString();
	}

}
