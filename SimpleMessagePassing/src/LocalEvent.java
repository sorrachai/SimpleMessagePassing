import java.io.Serializable;

//import org.jgroups.Message;

public class LocalEvent implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1041937224082446109L;
	public LocalEvent(EventType type, Timestamp localTimestamp, long localWallClock) {
		this.type = type;
		this.localTimestamp = new HybridVectorClock((HybridVectorClock)localTimestamp);
		this.localWallClock = localWallClock; 
	}
	public final EventType type;
	public final Timestamp localTimestamp;
	public final long localWallClock;  

}
