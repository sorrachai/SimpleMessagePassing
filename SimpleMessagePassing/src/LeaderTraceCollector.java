import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;  
import java.time.Duration;
import java.time.Instant;

public class LeaderTraceCollector {

	public LeaderTraceCollector(int numProcesses, Instant initialL) { 
		globalTrace = new ArrayList<>();
		globalHvcTrace = new ArrayList<>(); 
		globalHvcSizeOverEpsilon = new ArrayList<>();
		globalHvcSizeOverTime = new ArrayList<>();
		globalHvcSizeOverEpsilonNumEvents = new ArrayList<>();
		globalHvcSizeOverEpsilonDomain = new ArrayList<>();
		globalHvcSizeOverTimeDomain = new ArrayList<>();
		 
		
		globalTraceCounter=0;
		this.numProcesses = numProcesses;
		this.initialL = initialL;
		for(int i=0;i<numProcesses;i++) {
			globalTrace.add(new ArrayList<>());
			globalHvcTrace.add(new ArrayList<>());
			globalHvcSizeOverTime.add(new ArrayList<>());
			globalHvcSizeOverEpsilon.add(new ArrayList<>());
			globalHvcSizeOverEpsilonNumEvents.add(new ArrayList<>());
			globalHvcSizeOverEpsilonDomain.add(new ArrayList<>());
			globalHvcSizeOverTimeDomain.add(new ArrayList<>());
		}
		
		globalHvcSizeHistogram = new int[numProcesses];
        for(int i=0;i<this.numProcesses;i++) {
        	globalHvcSizeHistogram[i] = 0;
        }
        globalNumSentMessages=0;
        
	} 
	
 
	public void addHvcSizeOverEpsilon(LocalTraceCollector in,int from) {
		try {  
			globalHvcSizeOverEpsilon.get(from).addAll(in.getHvcSizeOverEpsilon());
			globalHvcSizeOverEpsilonNumEvents.get(from).addAll(in.getHvcSizeOverEpsilonNumEvents());
			globalHvcSizeOverEpsilonDomain.get(from).addAll(in.getHvcSizeOverEpsilonDomain());
			
			/*System.out.println("printing over time");
			for(int n : in.getHvcSizeOverTime()) {
				System.out.println(n);
			}
			System.out.println("printing over epsilon");
			for(int n : in.getHvcSizeOverEpsilon()) {
				System.out.println(n); 
			}
			System.out.println("---");*/
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	public void addTraceFrom(LocalTraceCollector in, int from) {
		try {
			globalTraceCounter++;
			System.out.println(globalTraceCounter);
			//globalTrace.get(from).addAll(in.getLocalTrace()); 
			//globalHvcTrace.get(from).addAll(in.getHvcTrace());
			globalHvcSizeOverTime.get(from).addAll(in.getHvcSizeOverTime());
			globalHvcSizeOverTimeDomain.get(from).addAll(in.getHvcSizeOverTimeDomain());
			globalNumSentMessages+=in.getNumSentMessages();
			int localHvcSizeHistogram [] = in.getHvcSizeHistogram();
			for(int i=0;i<this.numProcesses;i++) {
				globalHvcSizeHistogram[i] += localHvcSizeHistogram[i];
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	} 
	public boolean hasReceivedFromAllMembers() { 
		return globalTraceCounter==numProcesses-1;		
	}
	public void printGlobalTrace() {
		int i=0;
		for(ArrayList<LocalEvent> ve : globalTrace) {
			System.out.println("Printing event from process : " + Integer.toString(i++));
			for(LocalEvent e : ve) {
				e.localTimestamp.print();
			}
			System.out.println("---------");
		}
	}
	
	
	public void writeHvcSizeOverTimeAvgToFile(String name)  {

		try {
			FileWriter file = new FileWriter("./" + name,false);
			int traceLength = globalHvcSizeOverTime.get(1).size();
			
			for(int i=0;i<traceLength;i++) {
				double sum = 0;
			 
				for(int j=1;j<numProcesses;j++) {
					sum += globalHvcSizeOverTime.get(j).get(i);
			
				}
				sum = sum/(numProcesses-1);
				file.write((Duration.between(initialL, globalHvcSizeOverTimeDomain.get(1).get(i))).toMillis()+ " "+ Double.toString(sum));
				file.write(System.getProperty( "line.separator" ));
			}
			
			file.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void writeHvcSizeOverTimeRawToFile(String name)  {

		try {
			FileWriter file = new FileWriter("./" + name,false);
			int traceLength = globalHvcSizeOverTime.get(1).size();
			
			
			for(int i=0;i<traceLength;i++) { 
				file.write((Duration.between(initialL, globalHvcSizeOverTimeDomain.get(1).get(i))).toMillis()+ " ");
				for(int j=1;j<numProcesses;j++) {
					file.write(globalHvcSizeOverTime.get(j).get(i)+" ");
				}
				file.write(System.getProperty( "line.separator" ));
			}
			
			file.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	public void writeHvcSizeHitogramSnapsnotToFile(String name) {
		 
		int [] frequency = new int[numProcesses];
		int traceLength = globalHvcSizeOverTime.get(1).size();	
		int totalFrequency =0;
		for(int i=0;i<traceLength;i++) {
			for(int j=1;j<numProcesses;j++) {
				frequency[globalHvcSizeOverTime.get(j).get(i)]++;
				totalFrequency++;
			}
		}
		SimpleMessageUtilities.writeHistogramToFile(name, frequency, totalFrequency);
		
	}
 	
	public void writeHvcSizeHistogramToFile(String name) {
		
		int totalNumEvents = 0;
		for(int i=1;i<this.numProcesses;i++) {
			totalNumEvents += globalHvcSizeHistogram[i]; 
		}
		SimpleMessageUtilities.writeHistogramToFile(name, globalHvcSizeHistogram, totalNumEvents);
	}
 	
	public void writeHvcSizeOverEpsilonToFile(String name) {
		try {
			FileWriter file = new FileWriter("./" + name,false);
			int traceLength = globalHvcSizeOverEpsilon.get(1).size();
			
			System.out.println(traceLength);
			for(int i=0;i<traceLength;i++) {
				double sum = 0;
				int num_events = 0;
			
				for(int j=1;j<numProcesses;j++) {
					sum += globalHvcSizeOverEpsilon.get(j).get(i);
					num_events += globalHvcSizeOverEpsilonNumEvents.get(j).get(i);
				}
				sum = sum/num_events;
				//sum = sum/(numProcesses-1);
				file.write(globalHvcSizeOverEpsilonDomain.get(1).get(i)+" "+Double.toString(sum));
				file.write(System.getProperty( "line.separator" ));
			}
			file.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void printTotalNumSentMessages() {
		System.out.println("Total messages is " + globalNumSentMessages);
	}

	private Instant initialL;
	private int numProcesses;
	private int globalTraceCounter;
	private ArrayList<ArrayList<LocalEvent>> globalTrace;
	private ArrayList<ArrayList<LocalEvent>> globalHvcTrace;
	private ArrayList<ArrayList<Integer>> globalHvcSizeOverTime;
	private ArrayList<ArrayList<Integer>> globalHvcSizeOverEpsilon;
	private ArrayList<ArrayList<Long>> globalHvcSizeOverEpsilonDomain;
	private ArrayList<ArrayList<Instant>> globalHvcSizeOverTimeDomain;
	private ArrayList<ArrayList<Integer>> globalHvcSizeOverEpsilonNumEvents;
	private int [] globalHvcSizeHistogram;
	private int globalNumSentMessages;
	

}
