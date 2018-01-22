import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;

public class LocalStatisticsCollector implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -2380259998135200125L;
	public LocalStatisticsCollector() {
		localRTTs = new ArrayList<>();
	}
	private long computeMedian() {
		if(localRTTs.isEmpty()) return -1; 
		if(localRTTs.size()%2==1) {
			return localRTTs.get(localRTTs.size()/2);
		} 
		return Math.round((localRTTs.get(localRTTs.size()/2)+localRTTs.get(localRTTs.size()/2-1))/2.0);
	}
	private long computeQ1() {
		if(localRTTs.isEmpty()) return -1; 
		return Math.round((localRTTs.get(localRTTs.size()/4)));
	}
	private long computeQ3() {
		if(localRTTs.isEmpty()) return -1; 
		return Math.round((localRTTs.get((localRTTs.size()*3)/4)));
	}
	public void printStatistics() {
		Collections.sort(localRTTs); 
		System.out.println("-----------------");			
		System.out.println("| Average Latency: " + Math.round((double)sumLocalRTTs/(double)numLocalRTTs) + " mu-sec");
		System.out.println("| Minimum Latency: " + minLocalRTT  + " mu-sec");
		System.out.println("| Q1 Latency: " + computeQ1()  + " mu-sec");
		System.out.println("| Median Latency: " + computeMedian()  + " mu-sec");
		System.out.println("| Q3 Latency: " + computeQ3()  + " mu-sec");
		System.out.println("| Maximum Latency: " + maxLocalRTT + " mu-sec");
		System.out.println("-----------------");
	}
	public void logStatistics(FileWriter outputLog) throws IOException {
		 Collections.sort(localRTTs); 
		outputLog.write("---------------- ");
		outputLog.write(System.getProperty( "line.separator" ));
		outputLog.write("| Average Latency: " + Math.round((double)sumLocalRTTs/(double)numLocalRTTs) + " mu-sec");
		outputLog.write(System.getProperty( "line.separator" ));
		outputLog.write("| Minimum Latency: " + minLocalRTT  + " mu-sec");
		outputLog.write(System.getProperty( "line.separator" ));
 
		outputLog.write("| Q1 Latency: " + computeQ1()  + " mu-sec");
		outputLog.write(System.getProperty( "line.separator" ));
		outputLog.write("| Median Latency: " + computeMedian()  + " mu-sec");
		outputLog.write(System.getProperty( "line.separator" ));
		outputLog.write("| Q3 Latency: " + computeQ3()  + " mu-sec");
		outputLog.write(System.getProperty( "line.separator" ));
		outputLog.write("| Maximum Latency: " + maxLocalRTT + " mu-sec");
		outputLog.write(System.getProperty( "line.separator" ));
		outputLog.write("-----------------");
		outputLog.write(System.getProperty( "line.separator" ));
		
	}
	public void pushLocalStatistics(LocalStatisticsCollector in) {
		sumLocalRTTs += in.sumLocalRTTs;
		numLocalRTTs += in.numLocalRTTs;
		minLocalRTT = Math.min(in.minLocalRTT,minLocalRTT);
		maxLocalRTT = Math.max(in.maxLocalRTT,maxLocalRTT);
		numReceived++;
		this.localRTTs.addAll(in.localRTTs);
	}
	public boolean numReceivedEqualTo(int numWorkers) {
		return numReceived==numWorkers;
	}
	public void updateLocalRTTs(long newRTT) {
		sumLocalRTTs += newRTT;
		numLocalRTTs++;
		minLocalRTT = Math.min(newRTT, minLocalRTT);
		maxLocalRTT = Math.max(newRTT,maxLocalRTT);
		localRTTs.add(newRTT);
	}
	private long sumLocalRTTs =0;
	private long numLocalRTTs =0; 
	private long minLocalRTT = Long.MAX_VALUE; 
	private long maxLocalRTT = Long.MIN_VALUE;
	private int numReceived = 0;
	private ArrayList<Long> localRTTs;
	
}
