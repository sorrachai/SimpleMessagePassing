//import org.jgroups.Message;

public class LocalEvent {
	public LocalEvent(EventType type, Timestamp localTimestamp, long localWallClock) {
		this.type = type;
		this.localTimestamp = localTimestamp;
		this.localWallClock = localWallClock; 
	}
	public EventType type;
	public Timestamp localTimestamp;
	public long localWallClock; //System.currentTimeMillis();
	//public Message msg;

}
