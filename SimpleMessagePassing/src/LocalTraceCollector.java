import java.io.Serializable; 
import java.util.ArrayList;
import java.util.Date;;

public class LocalTraceCollector implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -955345333561783777L;
	public LocalTraceCollector() {
		localTrace = new ArrayList<>();
		hvcTrace = new ArrayList<>();
		hvcSizeOverTime = new ArrayList<>();
	}
	
	public ArrayList<LocalEvent> getLocalTrace() {
		return localTrace;
	}
	public ArrayList<LocalEvent> getHvcTrace() {
		return hvcTrace;
	}
	public ArrayList<Integer> getHvcSizeOverTime() {
		return hvcSizeOverTime;
	}
	public void pushLocalTrace(LocalEvent e) {
		localTrace.add(e);
	}
	public void fillHvcTrace(long initTime, long period, long stopTime) {
		 if(localTrace.isEmpty()) return;
		
		 int dummySize = 0;
		 
		 for(long tempTime =initTime; tempTime < stopTime; tempTime+=period ) {
			  dummySize++; 
			  
		 } 
		 long [] dummyTrace = new long[dummySize];
		 long tempTime = initTime;
		 for(int i=0;i<dummySize;i++) {
			  dummyTrace[i] = tempTime;
			  tempTime+=period;
		 }
		 
		 int localTracePtr = 0;
		 int dummyTracePtr = 0; 
		  
		 while(true) {
			 if(localTracePtr >= localTrace.size()) 
				 break;
			 if(dummyTracePtr >= dummySize) {
				 break;
			 }
			 Timestamp thisTimestamp = localTrace.get(localTracePtr).localTimestamp; 
			 long localL =  thisTimestamp.getL();
			 long dummyL = dummyTrace[dummyTracePtr];
			 if(dummyL > localL) {
				 localTracePtr++;
			 } else if (dummyL < localL || (dummyL == localL && thisTimestamp.getC() != 0)) {
				 int preLocalTracePtr = Math.max(localTracePtr-1,0);
				 HybridVectorClock hvcDummy = new HybridVectorClock((HybridVectorClock)localTrace.get(preLocalTracePtr).localTimestamp);
				 hvcDummy.timestampDummyEvent(dummyL);
				 hvcTrace.add(new LocalEvent(EventType.LOCAL_EVENT,hvcDummy,dummyL));
				 dummyTracePtr++;
			 } else {
				 HybridVectorClock hvcDummy = new HybridVectorClock((HybridVectorClock)thisTimestamp);
				 hvcDummy.timestampDummyEvent(dummyL);
				 hvcTrace.add(new LocalEvent(EventType.LOCAL_EVENT,hvcDummy,dummyL));
				 dummyTracePtr++;
				 localTracePtr++;
			 }
 		 } 
	}	
	public void computeHvcSizeOverTime() {
		hvcSizeOverTime = new ArrayList<>();
		//ArrayList<Long> tlong = new ArrayList<>();
		for(LocalEvent e : hvcTrace) {
			//tlong.add(e.localWallClock);
			hvcSizeOverTime.add(((HybridVectorClock)(e.localTimestamp)).getNumberActiveEntries());
		}
	/*	System.out.print("HVC_SIZE TIME \n[ ");
		long first_time = tlong.get(0);
		for(long n : tlong) {
			System.out.print( Long.toString(n) + "\n");
		}
		System.out.println("]");
		
		System.out.print("HVC_SIZE OVERTIME \n[ ");
		for(int n : hvcSizeOverTime) {
			System.out.print(Integer.toString(n) + " ");
		}
		System.out.println("]");*/
	}
	public void printLocalTrace() {
		for(LocalEvent e : localTrace) {
			e.localTimestamp.print();
		}
	}
	private ArrayList<LocalEvent> hvcTrace;
	private ArrayList<LocalEvent> localTrace;
	private ArrayList<Integer> hvcSizeOverTime; 
}
