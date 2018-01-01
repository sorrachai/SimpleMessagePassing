import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList; 

public class LocalTraceCollector implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -955345333561783777L;
	public LocalTraceCollector(int numberOfMembers) {
		this.numberOfMembers = numberOfMembers;
		localTrace = new ArrayList<>();
		hvcTrace = new ArrayList<>();
		hvcSizeOverTime = new ArrayList<>();
		hvcSizeOverEpsilon = new ArrayList<>();
		hvcSizeOverEpsilonNumEvents = new ArrayList<>();
        hvcSizeOverTimeDomain = new ArrayList<>();
        hvcSizeOverEpsilonDomain = new ArrayList<>();
        hvcSizeHistogram = new int[numberOfMembers+1];
        for(int i=0;i<this.numberOfMembers;i++) {
        		hvcSizeHistogram[i] = 0;
        }
        numSentMessages=0;
	}
    public int getNumSentMessages() {
    	return numSentMessages;
    }
	public int [] getHvcSizeHistogram() {
		return hvcSizeHistogram;
	}
	public ArrayList<LocalEvent> getLocalTrace() {
		return this.localTrace;
	}
	public ArrayList<LocalEvent> getHvcTrace() {
		return this.hvcTrace;
	}
	public ArrayList<Integer> getHvcSizeOverEpsilonNumEvents() {
		return this.hvcSizeOverEpsilonNumEvents;
	}
	public ArrayList<Integer> getHvcSizeOverTime() {
		return this.hvcSizeOverTime;
	}
	public ArrayList<Integer> getHvcSizeOverEpsilon() {
		return this.hvcSizeOverEpsilon;
	}
	public ArrayList<Long> getHvcSizeOverEpsilonDomain() {
		return this.hvcSizeOverEpsilonDomain;
	}
	public ArrayList<Instant> getHvcSizeOverTimeDomain() {
		return this.hvcSizeOverTimeDomain;
	}
	public void pushLocalTrace(LocalEvent e) {
		localTrace.add(e);
		if(e.type==EventType.SEND_MESSAGE || e.type==EventType.RECEIVE_MESSAGE || e.type == EventType.LOCAL_EVENT) {
			int numEntries = ((HybridVectorClock)(e.localTimestamp)).getNumberActiveEntries();
			hvcSizeHistogram[numEntries]++;
		}
		if(e.type==EventType.SEND_MESSAGE) {
			numSentMessages++;
		}
	}
	
	public void fillHvcTrace(Instant initTime, long period, Instant stopTime) {
		 if(localTrace.isEmpty()) return;
		
		 int dummySize = 0;
		 
		 for(Instant tempTime =initTime; tempTime.isBefore(stopTime); tempTime= tempTime.plusMillis(period)) {
			  dummySize++; 
		 } 
		 
		 Instant [] dummyTrace = new Instant[dummySize];
		 Instant tempTime = initTime;
		 for(int i=0;i<dummySize;i++) {
			  dummyTrace[i] = tempTime;
			  tempTime= tempTime.plusMillis(period); 
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
			 Instant localL =  thisTimestamp.getL();
			 Instant dummyL = dummyTrace[dummyTracePtr];
			 if(dummyL.isAfter(localL)) {
				 localTracePtr++;
			 } else if (dummyL.isBefore(localL) || (dummyL.equals(localL) && thisTimestamp.getC() != 0)) {
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
	//	hvcSizeOverTime = new ArrayList<>();
		//ArrayList<Long> tlong = new ArrayList<>();
		for(LocalEvent e : hvcTrace) {
			//tlong.add(e.localWallClock);
			hvcSizeOverTime.add(((HybridVectorClock)(e.localTimestamp)).getNumberActiveEntries());
			hvcSizeOverTimeDomain.add(e.localWallClock);
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
	public void computeHvcSizeOverEpsilon(long startEpsilon, long incrementInterval, long stopEpsilon) {
		 
		 
		ArrayList<LocalEvent> subHvcTrace = new ArrayList<LocalEvent>(hvcTrace.subList(Math.min(0, hvcTrace.size()), hvcTrace.size()));
		for(long eps = startEpsilon; eps <= stopEpsilon; eps += incrementInterval ) {
			int sumHVCSize = 0; 
			int numEvents = 0;
			for(LocalEvent e : subHvcTrace) {
				sumHVCSize +=  ((HybridVectorClock)(e.localTimestamp)).getNumberActiveEntries(eps);
				numEvents++;
			}
			//hvcSizeOverEpsilon += Integer.toString(sumHVCSize)+" ";
			hvcSizeOverEpsilon.add(sumHVCSize);
			hvcSizeOverEpsilonNumEvents.add(numEvents);
			hvcSizeOverEpsilonDomain.add(eps);
			//hvcSizeOverEpsilonNumEvents += Integer.toString(numEvents)+" ";
		} 
	 
	}
	
	public void printLocalTrace() {
		for(LocalEvent e : localTrace) {
			e.localTimestamp.print();
		}
	}
	private int numberOfMembers;
	private ArrayList<LocalEvent> hvcTrace;
	private ArrayList<LocalEvent> localTrace;
	private ArrayList<Integer> hvcSizeOverTime;
	private ArrayList<Instant> hvcSizeOverTimeDomain;
	private ArrayList<Integer> hvcSizeOverEpsilon;  
	private ArrayList<Long> hvcSizeOverEpsilonDomain; 
	private ArrayList<Integer> hvcSizeOverEpsilonNumEvents; 
	private int [] hvcSizeHistogram;
	private int numSentMessages;
}
