

public class Packet implements java.io.Serializable {

	private static final long serialVersionUID = -8815078910306869742L;
	
	private MessageType type;
	private Timestamp localTimestamp;
	private LocalTraceCollector allLocalEvents;
	private int indexFrom;
	private RunningParameters parameters; 
	private double ntpOffset;
	  
	  public Packet(MessageType type, double ntpOffset) {
		  if(type == MessageType.REPLY_NTP) {
			  this.type = type;
			  this.ntpOffset = ntpOffset;
		  } else {
			  this.type = MessageType.IGNORE;
		  }
	  }
	  public Packet(MessageType type, Timestamp localTimestamp) {
		  if(type == MessageType.NORMAL_RECEIVE) {
			  this.type = type;
			  this.localTimestamp = localTimestamp;
		  } else {
			  this.type = MessageType.IGNORE;
		  }
	  }
	  public Packet(MessageType type, LocalTraceCollector allLocalEvents,int indexFrom) {
		  if(type==MessageType.CONFIG_FINISH) {
			  this.type = type;
		  	  this.allLocalEvents = allLocalEvents; 
		  	  this.indexFrom = indexFrom;
		  } else {
			  this.type = MessageType.IGNORE;
		  }
	  }
	  public Packet(MessageType type, RunningParameters parameters) {
		  if(type==MessageType.CONFIG_START) {
			  this.type = type;  
			  this.parameters = parameters;
		  } else {
			  this.type = MessageType.IGNORE;
		  }
	  }
	  public Packet(MessageType type) {
		   this.type = type;   
	  }
	  public double getNtpOffset() { 
		  return ntpOffset;
	  }
	  public RunningParameters getRunningParameter() {
		  return parameters;
	  }
	  public MessageType getMessageType() {
		  return type;
	  }
	  public Timestamp getLocalTimestamp() {
		  return localTimestamp;
	  }
	  public LocalTraceCollector getAllLocalEvents() {
		  return allLocalEvents;
	  }
	  public int getIndexFrom() {
		  return indexFrom;
	  }
}
