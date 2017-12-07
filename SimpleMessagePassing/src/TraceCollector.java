import java.util.*;

public class TraceCollector {

	//private Timestamp local; 
	public TraceCollector(int myIndexCluter, int numProcesses) {
		init(myIndexCluster,numProcesses);
	}
	private void init(int myIndexCluster,int numProcesses) {
		this.myIndexCluster = myIndexCluster;
		localTrace = new Vector<>();
		if(this.myIndexCluster==0) {
			globalTrace = new Vector<>();
			globalTrace.addElement(new Vector<>());
			isLeader = true;
			globalTraceCounter=0;
			this.numProcesses = numProcesses;
			for(int i=0;i<numProcesses;i++) {
				globalTrace.addElement(new Vector<>());
			}
		}  
	}
	public Vector<LocalEvent> getAllLocalEvents() {
		return localTrace;
	}
	public void pushLocalEvent(LocalEvent e) {
		localTrace.addElement(e);
	}
	public void addTraceFrom(Vector<LocalEvent> in, int from) {
		if(this.isLeader) {
			globalTrace.elementAt(from).addAll(in);
			globalTraceCounter++;
		}
	} 
	public boolean hasReceivedFromAllMembers() {
		if(this.isLeader) {
			return globalTraceCounter==numProcesses-1;
		}
		//non-leader should not do anything 
		return false;
	}
	public boolean globalTraceIsVerified() {
		if(this.isLeader) {
			return true;
		}
		//non-leader should say yes so that we need to only check the leader. 
		return true;
	}
	private boolean isLeader=false;
	private int myIndexCluster; 
	private int numProcesses;
	private int globalTraceCounter=0;
	private Vector<Vector<LocalEvent>> globalTrace;
	private Vector<LocalEvent> localTrace;
	//private Vector
}
