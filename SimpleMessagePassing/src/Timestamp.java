import java.time.Instant;

public class Timestamp implements java.io.Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1239500822114531635L;
	public Timestamp() {
		initHLC();
		type = TimestampType.NO_TIMESTAMP;
	}
	public Timestamp(Timestamp that) {
		
		this.type = that.type;
		this.l = that.l;
		this.c = that.c;
		
		switch(type) {
			case VC: 
				this.localCausalityClock = new VectorClock((VectorClock)that.localCausalityClock);
				break;
			case HVC:
				this.localCausalityClock = new HybridVectorClock((HybridVectorClock)that.localCausalityClock);
				break;
			case STAT_HVC:
				this.localCausalityClock = new StatHVC((StatHVC)that.localCausalityClock);
				break;
			case HLC: 
				break;
			case NO_TIMESTAMP:
				break;
			default:
				break;
		}
	}
	
	public Timestamp(TimestampType type, int numProcesses, int myIndex, long epsilon) {
		
		this.type = type;
		initHLC();	
	 
		switch(type) {
			case VC: 
				this.localCausalityClock = new VectorClock(numProcesses, myIndex);
				break;
			case HVC:
				this.localCausalityClock = new HybridVectorClock(myIndex, epsilon);
				break;
			case STAT_HVC:
				this.localCausalityClock = new StatHVC(numProcesses, myIndex, epsilon);
				break;
			case HLC: 
				break;
			case NO_TIMESTAMP:
				break;
			default:
				break;
		}
	}
	public TimestampType getType() {
		return type;
	}
	public void timestampSendEvent() {
		if(this.type == TimestampType.NO_TIMESTAMP) return;
		timestampSendEventHLC();
		if(this.type == TimestampType.HLC) return;
		localCausalityClock.timestampSendEvent();	
	}
    public void timestampReceiveEvent(Timestamp in) {
    		if(this.type == TimestampType.NO_TIMESTAMP) return;
		timestampReceiveEventHLC(in);
		if(this.type == TimestampType.HLC) return;
    		localCausalityClock.timestampReceiveEvent(in.localCausalityClock);
    }
    public void timestampLocalEvent() {
    	    if(this.type == TimestampType.NO_TIMESTAMP) return;
		timestampLocalEventHLC();
		if(this.type == TimestampType.HLC) return;
    		localCausalityClock.timestampLocalEvent();
    }
	public void timestampDummyEvent(Instant L) {
		if(this.type == TimestampType.NO_TIMESTAMP || this.type == TimestampType.HLC) return;
		localCausalityClock.timestampDummyEvent(L);
	}
	public int getNumberActiveEntries(long epsilon) {
		if (this.type == TimestampType.STAT_HVC)  {
			return ((StatHVC)(localCausalityClock)).getNumberActiveEntries(epsilon);
		} else {
			throw new UnsupportedOperationException();
		}
	}
	
	public int getNumberActiveEntries() {
		if(this.type == TimestampType.HLC) return 1;
		if(this.type == TimestampType.NO_TIMESTAMP) return 0;
		
		return localCausalityClock.getNumberActiveEntries();
	}
	
	public void print() {
		if(this.type == TimestampType.NO_TIMESTAMP || this.type == TimestampType.HLC) return;
		localCausalityClock.print();
		printHLC();
	}
	

	public Instant getL() {
		return l;
	}
	public long getC() {
		return c;
	}
	private void initHLC() {
		l = Instant.now();
		c = 0;
	}
	private void timestampSendEventHLC() {
		if(type == TimestampType.NO_TIMESTAMP) return;
		Instant temp_l = l;
		l = SimpleMessageUtilities.maxInstant(temp_l, Instant.now());
		if( l.equals(temp_l)) {
			c++;
		} else {
			c=0;
		}
	}
	private void timestampReceiveEventHLC(Timestamp m) {
		if(type == TimestampType.NO_TIMESTAMP) return;
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
	private void timestampLocalEventHLC() {
		if(type == TimestampType.NO_TIMESTAMP) return;
		timestampSendEventHLC();
	} 
	private void printHLC() {
		if(type == TimestampType.NO_TIMESTAMP) return;
		System.out.println("(L,C) = (" + l + "," + Long.toString(c) +")");
	}
	
	private TimestampType type;
	private CausalityClock localCausalityClock;
	
	//built-in HLC for getting a consistent snapshot
	private long c;
	private Instant l;
	
}
