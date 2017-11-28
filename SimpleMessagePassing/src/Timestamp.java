import org.jgroups.Message;

public interface Timestamp  extends java.io.Serializable {
	
	public void init(int numProcesses, int myIndex);
	public void timestampSendEvent();
	public void timestampReceiveEvent(Message m);
	public void timestampLocalEvent(); 
	public String getTime();
	public void print();
	
}
