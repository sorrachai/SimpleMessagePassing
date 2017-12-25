

public class VectorClock extends Timestamp{

	public VectorClock(int numProcesses, int myIndex) {
		super(numProcesses, myIndex); 
		init(numProcesses,myIndex);
	}

	public VectorClock(VectorClock that) {
		// TODO Auto-generated constructor stub
		super(that);
		init(that.numProcesses, that.myIndex);
		for(int i=0;i<numProcesses;i++) {
			this.entries[i] = that.entries[i]; 
		}
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 5629085746531436092L;
	 
	private void init(int numProcesses, int myIndex) {
		// TODO Auto-generated method stub
		this.numProcesses = numProcesses;
		entries = new int[numProcesses];
		this.myIndex = myIndex;
		for(int i=0;i<numProcesses;i++) {
			entries[i]=0;
		}
	}
	
	public void timestampDummyEvent(long t) {
		if(entries[myIndex] > t) System.out.println("Warning: entries[myIndex] > t in timestampDummyEvent" );;
		entries[myIndex] = Math.max(entries[myIndex], (int)t);
	}
	
	@Override
	public void timestampSendEvent() {
		// TODO Auto-generated method stub
		entries[myIndex]++;
		
	}

	@Override
	public void timestampReceiveEvent(Timestamp m) {
		// TODO Auto-generated method stub
		VectorClock fromMessage = (VectorClock) m;
		entries[myIndex]++;
		for(int i=0;i<entries.length;i++) {
			entries[i] = Math.max(entries[i], fromMessage.entries[i]);
		}
	}

	@Override 
	public void timestampLocalEvent() {
		// TODO Auto-generated method stub
		entries[myIndex]++;
	}
	
	@Override
	public void print() {
		String out= new String();
		out += "[";
		for(int i=0;i<entries.length;i++) {
			out += Integer.toString(entries[i])+",";
		}
		out += "]";
		System.out.println(out);
	}
	 
	private int[] entries;
	private int myIndex;
	private int numProcesses;
}
