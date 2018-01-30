import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Vector;

import org.jgroups.Address; 

public class LocalTraceCollector implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -955345333561783777L;
	public LocalTraceCollector(int numberOfMembers, Address leader) {
		this.numberOfMembers = numberOfMembers;
		localTrace = new Vector<>();
		hvcTrace = new ArrayList<>();
		hvcSizeOverTime = new ArrayList<>();
		hvcSizeOverEpsilon = new ArrayList<>();
		hvcSizeOverEpsilonNumEvents = new ArrayList<>();
        hvcSizeOverTimeDomain = new ArrayList<>();
        hvcSizeOverEpsilonDomain = new ArrayList<>();
        hvcSizeHistogram = new int[numberOfMembers+1];
        messageSizes = new ArrayList<>();
        for(int i=0;i<this.numberOfMembers;i++) {
        		hvcSizeHistogram[i] = 0;
        }
        numRecvMessages=0;
        this.leader = leader;
        cValueHistogram = new Vector<>();
        for(int i = 0; i< 101;i++) {
        		cValueHistogram.add(0);
        }
	}
	public ArrayList<Long> getMessageSizes() {
		return messageSizes;
	}
    public int getNumRecvMessages() {
   // 	System.out.println("NumSentMessages = " + numSentMessages);
    	return numRecvMessages;
    }
	public int [] getHvcSizeHistogram() {
		return hvcSizeHistogram;
	}
	public Vector<LocalEvent> getLocalTrace() {
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
		/*if(e.localCausalityClock.getType()==TimestampType.HLC) {
			int c = (int)e.localCausalityClock.getC();
			cValueHistogram.set(c, cValueHistogram.get(c)+1);
		}*/
		/*if(e.type==EventType.SEND_MESSAGE || e.type==EventType.RECEIVE_MESSAGE || e.type == EventType.LOCAL_EVENT) {
			int numEntries = ((StatHVC)(e.localTimestamp)).getNumberActiveEntries();
			hvcSizeHistogram[numEntries]++;
		}*/
		/*if(e.eventType==EventType.SEND_MESSAGE) {
			numSentMessages++;
			long messagesSize = SimpleMessageUtilities.getOobMessage(leader, new Packet(MessageType.NORMAL_RECEIVE,e.localCausalityClock)).size();
			messageSizes.add(messagesSize);
		}*/
	} 
/*	public void pushLocalTraceSend(LocalEvent e) {
		localTrace.add(e);
		numSentMessages++;
		long messagesSize = SimpleMessageUtilities.getOobMessage(leader, new Packet(MessageType.NORMAL_RECEIVE,e.localCausalityClock)).size();
		messageSizes.add(messagesSize);

	}*/ 
	public void pushLocalTraceReceive(LocalEvent e) {
		localTrace.add(e);
		numRecvMessages++;
		long messagesSize = SimpleMessageUtilities.getOobMessage(leader, new Packet(MessageType.NORMAL_RECEIVE,e.localCausalityClock)).size();
		//long messagesSize = new Message(leader, "test").size();
		messageSizes.add(messagesSize);
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
			 Timestamp thisTimestamp = localTrace.get(localTracePtr).localCausalityClock; 
			 Instant localL =  thisTimestamp.getL();
			 Instant dummyL = dummyTrace[dummyTracePtr];
			 if(dummyL.isAfter(localL)) {
				 localTracePtr++;
			 } else if (dummyL.isBefore(localL) || (dummyL.equals(localL) && thisTimestamp.getC() != 0)) {
				 int preLocalTracePtr = Math.max(localTracePtr-1,0);
				 Timestamp timestampDummy = new Timestamp(localTrace.get(preLocalTracePtr).localCausalityClock);
				 timestampDummy.timestampDummyEvent(dummyL);
				 hvcTrace.add(new LocalEvent(EventType.LOCAL_EVENT,timestampDummy,dummyL));
				 dummyTracePtr++;
			 } else {
				 Timestamp timestampDummy = new Timestamp(thisTimestamp);
				 timestampDummy.timestampDummyEvent(dummyL);
				 hvcTrace.add(new LocalEvent(EventType.LOCAL_EVENT,timestampDummy,dummyL));
				 dummyTracePtr++;
				 localTracePtr++;
			 }
 		 } 
	}	
	public void computeHvcSizeOverTime(long duration, long period) {
	//	hvcSizeOverTime = new ArrayList<>();
		//ArrayList<Long> tlong = new ArrayList<>();
		for(LocalEvent e : hvcTrace) {
			//tlong.add(e.localWallClock);
			hvcSizeOverTime.add(e.localCausalityClock.getNumberActiveEntries());
			hvcSizeOverTimeDomain.add(e.localWallClock.minusMillis(5000+duration));
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
				sumHVCSize +=  (e.localCausalityClock).getNumberActiveEntries(eps);
				numEvents++;
			}
			//hvcSizeOverEpsilon += Integer.toString(sumHVCSize)+" ";
			hvcSizeOverEpsilon.add(sumHVCSize);
			hvcSizeOverEpsilonNumEvents.add(numEvents);
			hvcSizeOverEpsilonDomain.add(eps);
			//hvcSizeOverEpsilonNumEvents += Integer.toString(numEvents)+" ";
			//System.out.println("AVERAGE HVC SIZE OVER eps= "+ eps + " is: " +sumHVCSize/numEvents);
		} 
		
	}
	
	public void printLocalTrace() {
		for(LocalEvent e : localTrace) {
			e.localCausalityClock.print();
		}
	}
	private int numberOfMembers;
	private transient Address leader;
	private ArrayList<LocalEvent> hvcTrace;
	private Vector<LocalEvent> localTrace;
	private Vector<Integer> cValueHistogram;
	private ArrayList<Integer> hvcSizeOverTime;
	private ArrayList<Instant> hvcSizeOverTimeDomain;
	private ArrayList<Integer> hvcSizeOverEpsilon;  
	private ArrayList<Long> hvcSizeOverEpsilonDomain; 
	private ArrayList<Integer> hvcSizeOverEpsilonNumEvents; 
	private int [] hvcSizeHistogram;
	private int numRecvMessages;
	private ArrayList<Long> messageSizes;
	
}
