import java.time.Instant;

public interface  CausalityClock  extends java.io.Serializable {
 
	 public void timestampSendEvent();
	 public void timestampReceiveEvent(CausalityClock causalityClock);
	 public void timestampLocalEvent(); 
	 public void timestampDummyEvent(Instant L);
	 public int getNumberActiveEntries();
	 public void print();
	
}
