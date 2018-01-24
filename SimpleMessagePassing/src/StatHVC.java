 
import java.time.Instant; 

public class StatHVC implements CausalityClock {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -1579263943393529589L;
	public StatHVC(int numProcesses, int myIndex, long epsilon) {
	//	super(numProcesses, myIndex); 
		init(numProcesses,myIndex,epsilon); 
	}

	public StatHVC(StatHVC that) {
		// TODO Auto-generated constructor stub
	//	super(that);
		init(that.numProcesses, that.myIndex,that.epsilon);
		for(int i=0;i<numProcesses;i++) {
			this.entries[i] = that.entries[i]; 
		}
		
	}
 
	private void init(int numProcesses, int myIndex, long epsilon) {
		// TODO Auto-generated method stub
		this.numProcesses = numProcesses;
		entries = new Instant[numProcesses];
		this.myIndex = myIndex;
		this.epsilon = epsilon;
		Instant now = Instant.now();
		for(int i=0;i<numProcesses;i++) {
			entries[i]= now.minusNanos(epsilon*1000);
		}
		entries[myIndex] = now;
		
	}
	public int getNumberActiveEntries() {
		//process 0 is the leader which is not participating message exchanges.
		int num_inactive =1;
		for(int i=1;i<numProcesses;i++) { 
			Instant plusEps = entries[i].plusNanos(epsilon*1000);
			if(entries[myIndex].isAfter(plusEps) ||  entries[myIndex].equals(plusEps)) {
				num_inactive++;
			}
			//if(entries[myIndex] - entries[i]>= epsilon) num_inactive++;
		}
		return numProcesses-num_inactive;
	}
	public int getNumberActiveEntries(long epsilon) {
		int num_inactive =1;
		for(int i=1;i<numProcesses;i++) { 
			Instant plusEps = entries[i].plusNanos(epsilon*1000);
			if(entries[myIndex].isAfter(plusEps) ||  entries[myIndex].equals(plusEps)) {
				num_inactive++;
			} 
		}
	//	System.out.println("numProcesses= "+numProcesses);
	//	System.out.println("numInactive= "+num_inactive);
		return numProcesses-num_inactive;
	}
	public void timestampDummyEvent(Instant t) {
		//if(entries[myIndex] > t) System.out.println("Warning: entries[myIndex] > t in timestampDummyEvent" );;
		entries[myIndex] = SimpleMessageUtilities.maxInstant(entries[myIndex], t);
	}
	
	@Override
	public void timestampSendEvent() {
		// TODO Auto-generated method stub
		entries[myIndex] = SimpleMessageUtilities.maxInstant(entries[myIndex].plusNanos(1),Instant.now());
			
	}

	@Override
	public void timestampReceiveEvent(CausalityClock m) {
		// TODO Auto-generated method stub
		StatHVC fromMessage = (StatHVC) m;	
		entries[myIndex] = SimpleMessageUtilities.maxInstant(entries[myIndex].plusNanos(1),Instant.now());
			
		for(int i=0;i<entries.length;i++) {
			entries[i] = SimpleMessageUtilities.maxInstant(entries[i], fromMessage.entries[i]);
			//entries[i] = Math.max(entries[i], fromMessage.entries[i]);
		}
	}

	@Override 
	public void timestampLocalEvent() {
		// TODO Auto-generated method stub
		entries[myIndex] = SimpleMessageUtilities.maxInstant(entries[myIndex].plusNanos(1),Instant.now());
	}
	
	@Override
	public void print() {
		//String out= new String();
		//out += "[";
		System.out.println("[");
		for(int i=1;i<entries.length;i++) { 
			System.out.println(entries[i]);
		}  
		System.out.println("]\n Number entries: ");
		System.out.println(getNumberActiveEntries());
	}
	 
	private Instant [] entries;
	private int myIndex;
	private int numProcesses;
	private long epsilon;

}
