import java.time.Instant;

public abstract class Timestamp  implements java.io.Serializable {
	 
	/**
	 * 
	 */
	private static final long serialVersionUID = -2684565411535156950L;

	public Timestamp(int numProcesses, int myIndex) { 
		initHLC();
	}
	public Timestamp(Timestamp that) {
		this.l = that.l;
		this.c = that.c;
	} 
	abstract public void timestampSendEvent();
	abstract public void timestampReceiveEvent(Timestamp timestamp);
	abstract public void timestampLocalEvent(); 
	abstract public void print();
	
	
	//default HLC for getting a consistent snapshot
	private long c;
	private Instant l;
	
	public Instant getL() {
		return l;
	}
	public long getC() {
		return c;
	}
	public void initHLC() {
		l = Instant.now();
		c = 0;
	}
	public void timestampSendEventHLC() {
		Instant temp_l = l;
		l = SimpleMessageUtilities.maxInstant(temp_l, Instant.now());
		if( l.equals(temp_l)) {
			c++;
		} else {
			c=0;
		}
	}
	public void timestampReceiveEventHLC(Timestamp m) {
		Instant temp_l = l;
		Instant l_m = m.getL();
		long c_m = m.getC();
		l = SimpleMessageUtilities.maxInstant(temp_l, SimpleMessageUtilities.maxInstant(l_m ,Instant.now()));
		
		if(l.equals(temp_l) && temp_l.equals(l_m)) {
			c = Math.max(c_m, c)+1;
		} else if (l.equals(temp_l)) {
			c++;
		} else if (l.equals(l_m)) {
			c = c_m +1;
		} else {
			c = 0;
		}
	}
	public void timestampLocalEventHLC() {
		timestampSendEventHLC();
	} 
	public void printHLC() {
		System.out.println("(L,C) = (" + l + "," + Long.toString(c) +")");
	}
	
}
