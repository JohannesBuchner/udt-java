package udt.util;

public class StatisticsHistoryEntry {

	private final Object[] values;

	private final long timestamp;

	private final boolean isHeading;
	
	public StatisticsHistoryEntry(boolean heading, Object ...values){
		this.values=values;
		this.isHeading=heading;
		this.timestamp=System.currentTimeMillis();
	}

	public StatisticsHistoryEntry(Object ...values){
		this(false,values);
	}

	/**
	 * output as comma separated list
	 */
	public String toString(){
		StringBuilder sb=new StringBuilder();
		if(!isHeading){
			sb.append(timestamp);
			for(Object val: values){
				sb.append(",").append(String.valueOf(val));
			}
		}
		else{
			for(int i=0;i<values.length;i++){
				if(i>0)sb.append(",");
				sb.append(String.valueOf(values[i]));
			}
		}
		return sb.toString();
	}

}
