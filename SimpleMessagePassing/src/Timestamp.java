

public abstract class Timestamp  implements java.io.Serializable {
	 
	/**
	 * 
	 */
	private static final long serialVersionUID = -2684565411535156950L;

	public Timestamp(int numProcesses, int myIndex) { 
		
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
	private long l,c;
	
	public long getL() {
		return l;
	}
	public long getC() {
		return c;
	}
	public void initHLC() {
		l = 0;
		c = 0;
	}
	public void timestampSendEventHLC() {
		long temp_l = l;
		l = Math.max(temp_l, System.currentTimeMillis());
		if(l==temp_l) {
			c++;
		} else {
			c=0;
		}
	}
	public void timestampReceiveEventHLC(Timestamp m) {
		long temp_l = l;
		long l_m = m.getL();
		long c_m = m.getC();
		l = Math.max(temp_l, Math.max(l_m ,System.currentTimeMillis()));
		
		if(l == temp_l && temp_l == l_m) {
			c = Math.max(c_m, c)+1;
		} else if (l == temp_l) {
			c++;
		} else if (l == l_m) {
			c = c_m +1;
		} else {
			c = 0;
		}
	}
	public void timestampLocalEventHLC() {
		timestampSendEventHLC();
	} 
	public void printHLC() {
		System.out.println("(L,C) = (" + Long.toString(l) + "," + Long.toString(c) +")");
	}
	
}
