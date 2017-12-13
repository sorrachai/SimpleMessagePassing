import java.util.Date;

public class HybridVectorClock extends Timestamp {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -1579263943393529589L;
	public HybridVectorClock(int numProcesses, int myIndex, long epsilon) {
		super(numProcesses, myIndex); 
		init(numProcesses,myIndex,epsilon); 
	}

	public HybridVectorClock(HybridVectorClock that) {
		// TODO Auto-generated constructor stub
		super(that);
		init(that.numProcesses, that.myIndex,that.epsilon);
		for(int i=0;i<numProcesses;i++) {
			this.entries[i] = that.entries[i]; 
		}
		
	}
 
	
	private void init(int numProcesses, int myIndex, long epsilon) {
		// TODO Auto-generated method stub
		this.numProcesses = numProcesses;
		entries = new long[numProcesses];
		this.myIndex = myIndex;
		this.epsilon = epsilon;
		long now = System.currentTimeMillis();
		for(int i=0;i<numProcesses;i++) {
			entries[i]= now-epsilon;
		}
		entries[myIndex] = now;
		
	}
	public int getNumberActiveEntries() {
		int num_inactive =0;
		for(int i=0;i<numProcesses;i++) { 
			if(entries[myIndex] - entries[i]>= epsilon) num_inactive++;
		}
		return numProcesses-num_inactive;
	}
	public void timestampDummyEvent(long t) {
		if(entries[myIndex] > t) System.out.println("Warning: entries[myIndex] > t in timestampDummyEvent" );;
		entries[myIndex] = Math.max(entries[myIndex], (int)t);
	}
	
	@Override
	public void timestampSendEvent() {
		// TODO Auto-generated method stub
		entries[myIndex] = Math.max(entries[myIndex]+1,System.currentTimeMillis());
		
	}

	@Override
	public void timestampReceiveEvent(Timestamp m) {
		// TODO Auto-generated method stub
		HybridVectorClock fromMessage = (HybridVectorClock) m;	
		entries[myIndex] = Math.max(entries[myIndex]+1,System.currentTimeMillis());
		for(int i=0;i<entries.length;i++) {
			entries[i] = Math.max(entries[i], fromMessage.entries[i]);
		}
	}

	@Override 
	public void timestampLocalEvent() {
		// TODO Auto-generated method stub
		entries[myIndex] = Math.max(entries[myIndex],System.currentTimeMillis());
	}
	
	@Override
	public void print() {
		//String out= new String();
		//out += "[";
		System.out.println("[");
		for(int i=0;i<entries.length;i++) {
			Date d = new Date(entries[i]);
			System.out.println(d);
		}  
		System.out.println("]\n Number entries: ");
		System.out.println(getNumberActiveEntries());
	}
	 
	private long [] entries;
	private int myIndex;
	private int numProcesses;
	private long epsilon;

}
