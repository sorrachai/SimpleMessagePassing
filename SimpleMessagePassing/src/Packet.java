import java.time.Instant;

public class Packet implements java.io.Serializable {

	private static final long serialVersionUID = -8815078910306869742L;
	
	private MessageType type;
	private Timestamp localTimestamp = null;
	private LocalTraceCollector allLocalEvents = null;
	private int indexFrom;
	private RunningParameters parameters = null;
	private double ntpOffset;
	private LocalStatisticsCollector localStat = null; 
	private Instant time = null;
	  
	  public Instant getTime() {
		  return time;
	  }
	  public Packet (MessageType type, LocalStatisticsCollector localStat) {
		  if(type==MessageType.COLLECT_LATENCY) {
			  this.type = type;
			  this.localStat = localStat;
		  } else {
			  this.type = MessageType.IGNORE;
		  }
	  }
	  public Packet(MessageType type, double ntpOffset) {
		  if(type == MessageType.REPLY_NTP) {
			  this.type = type;
			  this.ntpOffset = ntpOffset;
		  } else {
			  this.type = MessageType.IGNORE;
		  }
	  }
	  public Packet(MessageType type, Instant time, int indexFrom) {
		  if( type ==MessageType.PING_LATENCY ) {
			  this.type = type;
			  this.time  = time;
			  this.indexFrom = indexFrom;
		  } 	
		  else {
			  this.type = MessageType.IGNORE;
		  }
	  }
	  public Packet(MessageType type, Instant time) {
		  if( type ==MessageType.PONG_LATENCY ) {
			  this.type = type;
			  this.time  = time; 
		  } 	
		  else {
			  this.type = MessageType.IGNORE;
		  }
	  }
	  public Packet(MessageType type, Timestamp localTimestamp) {
		  if(type == MessageType.NORMAL_RECEIVE) {
			  this.type = type;
			  this.localTimestamp = localTimestamp;
			   time = null;
			   localStat = null;
			   allLocalEvents = null;
			   parameters = null;
		  } 	
		  else {
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
	  public LocalStatisticsCollector getLocalStatisticsCollector() {
		  return localStat;
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
