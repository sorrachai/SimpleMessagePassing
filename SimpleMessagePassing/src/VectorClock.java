import org.jgroups.Message;
import java.util.*;

public class VectorClock implements Timestamp{

	@Override
	public void init(int numProcesses, int myIndex) {
		// TODO Auto-generated method stub
		entries = new int[numProcesses];
		this.myIndex = myIndex;
		for(int i=0;i<numProcesses;i++) {
			entries[i]=0;
		}
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

	public String getTime() {
		// TODO Auto-generated method stub
		return null;
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
	
	private static final long serialVersionUID = 1L;
	private int[] entries;
	private int myIndex;
	
}
