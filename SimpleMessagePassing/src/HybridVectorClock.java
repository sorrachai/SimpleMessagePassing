import java.io.Serializable;
import java.time.Instant;
import java.util.Iterator;
import java.util.LinkedList; 
public class HybridVectorClock implements CausalityClock {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -1579263943393529589L;
	public HybridVectorClock(int myIndex, long epsilon) {
		init(myIndex,epsilon); 
	}

	@SuppressWarnings("unchecked")
	public HybridVectorClock(HybridVectorClock that) {
		init(that.myIndex,that.epsilon);
		this.activeEntries = (LinkedList<HvcEntry>) that.activeEntries.clone();
	}
 
	private void init(int myIndex, long epsilon) {
		// TODO Auto-generated method stub
		this.activeEntries = new LinkedList<>();
		this.myTime = Instant.now();
		this.myIndex = myIndex;
		this.activeEntries.add(new HvcEntry(myIndex,this.myTime));
		this.epsilon = epsilon; 
	}
	public int getNumberActiveEntries() {
		//process 0 is the leader which is not participating message exchanges.
		int size =0;
		for(@SuppressWarnings("unused") HvcEntry t : activeEntries) size++;
		return size;
	}

	private void refreshActiveEntries(Instant withTime) {
		myTime = withTime;
		for (Iterator<HvcEntry> iterator = activeEntries.iterator(); iterator.hasNext();) {
			HvcEntry e = iterator.next();
			if(e.index==this.myIndex) {
				e.val = myTime;
			} else {
				Instant plusEps = e.val.plusNanos(epsilon*1000);
				if(myTime.isAfter(plusEps)) {
					iterator.remove();
				}
			}
		}
	}
	public void timestampDummyEvent(Instant t) {
		//if(entries[myIndex] > t) System.out.println("Warning: entries[myIndex] > t in timestampDummyEvent" );;
		//entries[myIndex] = SimpleMessageUtilities.maxInstant(entries[myIndex], t);
		refreshActiveEntries(t);
	}
	
	@Override
	public void timestampSendEvent() { 
		
		refreshActiveEntries(Instant.now());
	}

	@Override
	public void timestampReceiveEvent(CausalityClock m) { 
		
		LinkedList<HvcEntry> activeEntriesfromMessage = ((HybridVectorClock) m).activeEntries;	
		refreshActiveEntries(Instant.now());
		Instant myTimeMinusEps = myTime.minusNanos(epsilon*1000);
		Iterator<HvcEntry> itrMessage = activeEntriesfromMessage.iterator();
		Iterator<HvcEntry> itrThis = this.activeEntries.iterator();
		
		LinkedList<HvcEntry> newActiveEntries = new LinkedList<>();
		
		//This works since the list must contain at least one entry which is itself
		HvcEntry entryThis = itrThis.next();
		HvcEntry entryMessage = itrMessage.next();
		
		boolean compareThis = false;
		boolean compareMessage = false;
		while(true) {
			if(entryThis.index < entryMessage.index) {
				if(entryThis.val.isAfter(myTimeMinusEps)) {
					newActiveEntries.add(new HvcEntry(entryThis));
				}
				if(itrThis.hasNext()) entryThis = itrThis.next();
				else  {
					compareMessage = true;
					break;
				}
			} else if(entryThis.index > entryMessage.index) {
				if(entryMessage.val.isAfter(myTimeMinusEps)) {
					newActiveEntries.add(new HvcEntry(entryMessage));	
				}
				if(itrMessage.hasNext()) entryMessage = itrMessage.next();
				else  {
					compareThis= true;
					break;
				}
			} else {
				Instant max = SimpleMessageUtilities.maxInstant(entryThis.val, entryMessage.val);
				if(max.isAfter(myTimeMinusEps)) {
					newActiveEntries.add(new HvcEntry(entryThis.index,max));	
				}
				if(!itrThis.hasNext() || !itrMessage.hasNext()) break;
				entryThis = itrThis.next();
			    entryMessage = itrMessage.next();
			}
		} 
		
		if(compareThis) {
			if(entryThis.val.isAfter(myTimeMinusEps)) {
				newActiveEntries.add(new HvcEntry(entryThis));
			}
		}
		
		if(compareMessage) {
			if(entryMessage.val.isAfter(myTimeMinusEps)) {
				newActiveEntries.add(new HvcEntry(entryMessage));	
			}
		}
	
		while(itrThis.hasNext()) {
			entryThis = itrThis.next();
			if(entryThis.val.isAfter(myTimeMinusEps)) {
				newActiveEntries.add(new HvcEntry(entryThis));
			}
		} 
		
		while(itrMessage.hasNext()) {
			entryMessage = itrMessage.next();
			if(entryMessage.val.isAfter(myTimeMinusEps)) {
				newActiveEntries.add(new HvcEntry(entryMessage));	
			}
		}
		
		activeEntries = newActiveEntries;
	}

	@Override 
	public void timestampLocalEvent() { 
		myTime = Instant.now();
		refreshActiveEntries(myTime);
		
	}
	
	@Override
	public void print() {
		//String out= new String();
		//out += "[";
		System.out.println("[");
		for(HvcEntry t : activeEntries) {
			System.out.println("("+t.index+", "+t.val+") ");
		}
		System.out.println("]\n Number entries: ");
		System.out.println(getNumberActiveEntries());
	}
	 
	private transient int myIndex; 
	private transient long epsilon;
	private transient Instant myTime; 
	
	private LinkedList<HvcEntry> activeEntries;
}

class HvcEntry implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = -8698661279064826655L;
	HvcEntry(HvcEntry that) {
		this.index = that.index;
		this.val = that.val;
	}
	HvcEntry(int index, Instant val) {
		this.index = index;
		this.val = val;
	}
	int index;
	Instant val;
}
