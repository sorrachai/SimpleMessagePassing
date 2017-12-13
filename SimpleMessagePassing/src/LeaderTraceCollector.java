import java.util.ArrayList;;

public class LeaderTraceCollector {

	public LeaderTraceCollector(int numProcesses) { 
		globalTrace = new ArrayList<>();
		globalHVCTrace = new ArrayList<>(); 
		
		globalTraceCounter=0;
		this.numProcesses = numProcesses;
		for(int i=0;i<numProcesses;i++) {
			globalTrace.add(new ArrayList<>());
			globalHVCTrace.add(new ArrayList<>());
		}
	} 
	
	public void addTraceFrom(LocalTraceCollector in, int from) {
		try {
			globalTraceCounter++;
			System.out.println(globalTraceCounter);
			globalTrace.get(from).addAll(in.getLocalTrace());
			globalHVCTrace.get(from).addAll(in.getHvcSizeOverTime());
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
 	private int numProcesses;
	private int globalTraceCounter;
	private ArrayList<ArrayList<LocalEvent>> globalTrace;
	private ArrayList<ArrayList<Integer>> globalHVCTrace;
}
