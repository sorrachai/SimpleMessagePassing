import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View; 

import java.util.ArrayList;
import java.util.Date;  

import java.time.Instant;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.Duration;

public class SimpleMessagePassing extends ReceiverAdapter {
	   
	 
	
	@SuppressWarnings("resource")
	private void start() throws Exception {
		//channel=new JChannel("tcp.xml").setReceiver(this);
		//channel=new JChannel("simpleMessageJgroupConfig.xml").setReceiver(this);
		channel=new JChannel().setReceiver(this);
        channel.connect("Cluster"); 
        
        localComputation();
        channel.close();
    }
	
	public void viewAccepted(View new_view) {
	//	System.out.println("** view: " + new_view);
	} 
									
	public void receive(Message msg) {
		
		try {
			Packet receivedPacket = (Packet) (msg.getObject());
			switch(receivedPacket.getMessageType()) {
				case START_LATENCY: 
					localStatisticsCollector = new LocalStatisticsCollector();
					localSetup();
					state = LocalState.GET_LATENCY;
					break;
			 	case PING_LATENCY: 
		 			Packet pong = new Packet(MessageType.PONG_LATENCY,receivedPacket.getTime()); 
		 			int receivedFrom = receivedPacket.getIndexFrom();
		 			channel.send(SimpleMessageUtilities.getOobMessage(members.get(receivedFrom), pong));
			 		break;
			 	case PONG_LATENCY:
			 		if(state == LocalState.EXECUTE_LATENCY_RUN  || state == LocalState.GET_LATENCY) {
			 			Duration RTT = Duration.between(receivedPacket.getTime(), Instant.now()); 
			 			try {
			 				long durationMicrosec = Math.round(RTT.toNanos()/(2*1000.0));
				 			synchronized(localStatisticsCollector) {
				 				localStatisticsCollector.updateLocalRTTs(durationMicrosec);
				 			} 
			 			} catch(Exception e) {
			 				e.printStackTrace();
			 			}
			 		}
			 		break;
			 	case COLLECT_LATENCY: 
			 		LocalStatisticsCollector receivedLocalStatistics = receivedPacket.getLocalStatisticsCollector();
			 		synchronized(localStatisticsCollector) {
			 			localStatisticsCollector.pushLocalStatistics(receivedLocalStatistics);
			 		}
			 		if(localStatisticsCollector.numReceivedEqualTo(numWorkers)) { 
			 			localStatisticsCollector.reportStatistics();
			 			System.out.println("packet size = " + SimpleMessageUtilities.getOobMessage(members.get(0), new Packet(MessageType.PONG_LATENCY,localClock)).size() + " Bytes");
			 			if(state != LocalState.SETUP_LATENCY_RUN) state = LocalState.IDLE;
			 		}
			 		break;
				case NORMAL_RECEIVE: 
					if(state==LocalState.EXECUTE_NORMAL_RUN ) {
						Timestamp t = receivedPacket.getLocalTimestamp();
						
						synchronized(localClock) {
							
							localClock.timestampReceiveEvent(t);
							
							/*
							System.out.println("Received: ");
							t.print();
							System.out.println("Before: ");
							localClock.print();
							localClock.timestampReceiveEvent(t);
							System.out.println("After receive");
							localClock.print(); */ 
						}
						
						localTraceCollector.pushLocalTrace(new LocalEvent(EventType.RECEIVE_MESSAGE,localClock,Instant.now()));
					}
					break;
				
				case CONFIG_START:
					parameters = new RunningParameters(receivedPacket.getRunningParameter());
					state = LocalState.SETUP_LATENCY_RUN;
					break;
				case CONFIG_STOP:
					state = LocalState.STOP;
					break;
				case CONFIG_FINISH: 
					System.out.print("Received CONFIG_FINISH ");
					LocalTraceCollector receivedLocalTrace = receivedPacket.getAllLocalEvents();
					int from = receivedPacket.getIndexFrom();
					synchronized(leaderTraceCollector) {
						if(parameters.runQuery) {
							leaderTraceCollector.addHvcSizeOverEpsilon(receivedLocalTrace, from);
						}
						leaderTraceCollector.addTraceFrom(receivedLocalTrace,from);
					}
					if ( leaderTraceCollector.hasReceivedFromAllMembers() ) {
						//leaderTraceCollector.printGlobalTrace();
						leaderTraceCollector.printStatistics(parameters);
						leaderTraceCollector.writeHvcSizeOverTimeRawToFile("HvcOverTimeRaw.out");
						leaderTraceCollector.writeHvcSizeOverTimeAvgToFile("HvcOverTimeAvg.out");
						leaderTraceCollector.writeHvcSizeHitogramSnapsnotToFile("HvcHistogram.out");
						if(parameters.runQuery) leaderTraceCollector.writeHvcSizeOverEpsilonToFile("HvcOverEpsilon.out");
						state = LocalState.IDLE;
					} 
					break;
				case REQUEST_INTERNET_NTP: 
					state = LocalState.GET_INTERNET_NTP;
					break;
				
				case REQUEST_AMAZON_NTP: 
					state = LocalState.GET_AMAZON_NTP;
					break;
				case REPLY_NTP:
					double localNtpOffset = receivedPacket.getNtpOffset();
					globalNtpOffset.add(localNtpOffset);
					if(globalNtpOffset.size()==numWorkers) {
						System.out.println("\n -------- listing offset -------- ");
						for(double d : globalNtpOffset) {
							System.out.print(d + " ");
						}
						System.out.println("\nAverage Offset is : " + SimpleMessageUtilities.average(globalNtpOffset));
						System.out.println("\nMax Offset is : " + SimpleMessageUtilities.max(globalNtpOffset));
						state = LocalState.IDLE;
						System.out.println("---------------- ");
					}
					break;
				case PING:
					System.out.println("");
					break;
				case IGNORE: 
					System.out.println("Warning: received IGNORE message");
				default:
					
				break;
			}
		} catch(Exception e) {
		  	e.printStackTrace();
		}
	}

	private int indexOfMyAddress(java.util.List<org.jgroups.Address> members)  {
	    int i =0;
	    org.jgroups.Address myAddress = (org.jgroups.Address) channel.getAddress();
		for(org.jgroups.Address addr : members) {
			//System.out.println(addr + " vs. " + myAddress);
			if(addr.toString()!=myAddress.toString()) {
				i++;
			}
			else {
				return i;
			}
		}
		return i;
	}

	
	
	private void localSetup(){
		this.members = channel.getView().getMembers();  
		this.myIndexOfTheGroup = indexOfMyAddress(members); 
		this.numWorkers = this.members.size()-1;
		this.localClock = new Timestamp(TimestampType.NO_TIMESTAMP,this.members.size(),myIndexOfTheGroup,1);
		
	}
	private void leaderSetup() {
		localSetup();
		localStatisticsCollector = new LocalStatisticsCollector();
		this.localClock = new Timestamp(parameters.timestampType,this.members.size(), myIndexOfTheGroup,parameters.globalEpsilon);
		parameters.setRandom(myIndexOfTheGroup);
		leaderTraceCollector = new LeaderTraceCollector(parameters.numberOfMembers,parameters.startTime.toInstant());
	}
	private void nonLeaderSetup() {
		localSetup();
		localStatisticsCollector = new LocalStatisticsCollector();
		this.localClock = new Timestamp(parameters.timestampType,this.members.size(), myIndexOfTheGroup,parameters.globalEpsilon);
		parameters.setRandom(myIndexOfTheGroup);
		localTraceCollector = new LocalTraceCollector(parameters.numberOfMembers);
	}
	private void printInstruction() {
		System.out.println("usage: start "
						+ "[int duration:secs] "
						+ "[int timeunit:micro secs] "
						+ "[unicast_probability:0-1] "
						+ "[long hvc_collecting_peroid:ms] "
						+ "[long epsilon:micro secs] "
						+ "[string uniform || zipf={double skew}]" 
						+ "[string query: no || yes={epsilon_start:epsilon_interval:epsilon_stop}"
						+ "[string causality_clock: {hvc,vc,hlc, stat_hvc, no_clock}");
		System.out.println("usage2: get num_nodes || ntp_internet (ms) || ntp_amazon (s) || group_latency (mu-s)");
		System.out.println("usage3: exit");
	}
	private boolean correctFormat(String[] cmd) {
		
		boolean lengthEqual9 = cmd.length == 9;
		if(!lengthEqual9) { 
			return false;
		}
		boolean firstisint = SimpleMessageUtilities.isInteger(cmd[1]);
		boolean secondisint = SimpleMessageUtilities.isInteger(cmd[2]);
		boolean thirdisreal = SimpleMessageUtilities.isNumeric(cmd[3]);
		boolean fourthislong = SimpleMessageUtilities.isLong(cmd[4]);
		boolean fifthislong = SimpleMessageUtilities.isLong(cmd[5]);
		
		if( firstisint && secondisint && thirdisreal && fourthislong && fifthislong)  {
			double unicastProbabilityMessage = Double.parseDouble(cmd[3]);
			if(unicastProbabilityMessage <= 0 || unicastProbabilityMessage >1)  {
				System.out.println("Need unicast_probability between 0 and 1");
				return false;
			}
		}
		
		String causalityClockString = cmd[8].toLowerCase().trim();
		if(causalityClockString.startsWith("hvc")) {	
			
		} else if(causalityClockString.startsWith("vc")) {
			
		} else if(causalityClockString.startsWith("hlc")) {	 
			
		} else if(causalityClockString.startsWith("stat_hvc")) {
			
		} else if(causalityClockString.startsWith("no")) {
			
		} else {
			return false;
		}
		
				
		return true;
	}
	
	private void broadcastCommand(String [] cmd)  throws Exception {
		double unicastProbabilityMessage = Double.parseDouble(cmd[3]);
		int durationMessage = (Integer.parseInt(cmd[1]))*1000;
		int timeunitMessage = Integer.parseInt(cmd[2]);
		Date startTimeMessage = Date.from(Instant.now().plusSeconds(10));
		long period = Long.parseLong(cmd[4]);
		long epsilon = Long.parseLong(cmd[5]);
		String destinationDistributionString = cmd[6].toLowerCase().trim();
		String queryString = cmd[7].toLowerCase().trim();
		String causalityClockString = cmd[8].toLowerCase().trim();
			
		int numberOfMembers = channel.getView().getMembers().size();
		//need seed to be less than 48 bits 
		long initialRandomSeed =  Instant.now().toEpochMilli()%1000003; 
		Packet packet = new Packet(MessageType.CONFIG_START,
							new RunningParameters( 
							    numberOfMembers,  
							    initialRandomSeed,
								unicastProbabilityMessage,
								timeunitMessage,
								durationMessage,
								startTimeMessage,
								period,
								epsilon,
								destinationDistributionString,
								queryString,
								causalityClockString
								) 
		);
	    channel.send(null,packet);
	    System.out.println("---------");
	    System.out.println("start messages have been sent.");
		System.out.println("With the following parameters:");
		System.out.println("StartTime = "+startTimeMessage);
		System.out.println("Duration = "+ cmd[1] + " s");
		System.out.println("Time Unit = "+cmd[2] + "mu-s");
		System.out.println("UnicastProbability = "+cmd[3]);
		System.out.println("Period = "+cmd[4] + "ms");
		System.out.println("Epsilon = " + cmd[5] + "mu-s");
		System.out.println(destinationDistributionString);
		System.out.println(queryString);
		System.out.println(causalityClockString);
		System.out.println("---------");
	}
		
	private void waitUntilReceivedAllLocalStates(LocalState s) throws Exception {
		while(state == s) {
			Thread.sleep(1000);
			System.out.print(".");
		} 
	}
	
	private void runLeaderRoutine() throws Exception {
		
		while(true) { 
			state = LocalState.IDLE;
			Thread.sleep(100); 
			printInstruction();
			System.out.print("> ");
			BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
			String line =in.readLine().toLowerCase();
			if(line.startsWith("start")) {
				String[] command = line.split(" ");
				if(!correctFormat(command)) {
					//printInstruction();
					continue;
				}
				broadcastCommand(command);
				while(state!=LocalState.SETUP_LATENCY_RUN);
				leaderSetup();		
				waitUntilReceivedAllLocalStates(LocalState.SETUP_LATENCY_RUN); 
				System.out.println("The execution has been completed.");
			}
			else if(line.startsWith("get")) {
				
				//System.out.println("usage2: info num_nodes || ntp_internet || ntp_amazon || ");
				String[] command = line.split(" ");
				boolean length2= command.length == 2;
				if(!length2) { 
					// printInstruction();
					 continue;
				}
				if(command[1].startsWith("num_node")) {
					System.out.println("Number of nodes including leader is " + channel.getView().getMembers().size());
					continue;
				}
				this.numWorkers  = channel.getView().getMembers().size()-1;
				System.out.println("Number of worker nodes is " + numWorkers);
				globalNtpOffset = new ArrayList<>();
				Packet packet;  
				if(command[1].startsWith("ntp_int")) {
					packet = new Packet(MessageType.REQUEST_INTERNET_NTP);
					//channel.send(SimpleMessageUtilities.getOobMessage(null, packet));
					channel.send(null,packet);
					state = LocalState.GET_INTERNET_NTP;
				
				}
				else if(command[1].startsWith("ntp_ama")) {
					packet = new Packet(MessageType.REQUEST_AMAZON_NTP);
					channel.send(null,packet);
					state = LocalState.GET_AMAZON_NTP;
				}
				else if (command[1].startsWith("group")) {
					packet = new Packet(MessageType.START_LATENCY);
					channel.send(null,packet);
					state = LocalState.GET_LATENCY;
				}
				else {
					//printInstruction();
					continue;
				} 
				while(state!=LocalState.IDLE) {
					Thread.sleep(10); 
				}
			}
			else if (line.startsWith("quit") || line.startsWith("exit")) {
				Packet packet = new Packet(MessageType.CONFIG_STOP);
				channel.send(null,packet);
				return;
			}
		}
	}
	private void nonLeaderRoutine() {
		 //the following loop is for non-leaders
		try {
			state = LocalState.IDLE;
			Instant initTime = Instant.now();
			Duration elapsedTime; //= initTime; 
			Instant initL = initTime;
			Instant lastL = initTime;
			boolean sendMessage;
			//local state may be changed upon receiving a message
			while(true) {
				switch(state) {
				case IDLE:  
					Thread.sleep(1000); 
					System.out.print(".");
					this.members = channel.getView().getMembers();  
					this.myIndexOfTheGroup = indexOfMyAddress(members); 
				/*	if(myIndexOfTheGroup==1) {
						Thread.sleep(5000); 
						//ping leader to let ssh connection stay alive
						 Packet pingpacket = new Packet(MessageType.PING);
						 channel.send(members.get(0),pingpacket);
					}*/
					//waiting for leader's command to run or to stop the programs.
					continue;
				case GET_INTERNET_NTP:
				case GET_AMAZON_NTP: 
					 this.members = channel.getView().getMembers();  
					 double ntpOffset; 
					 Packet packetinfo;
					 if(state==LocalState.GET_AMAZON_NTP) {
						 ntpOffset = SimpleMessageUtilities.getAmazonNtpOffset();
					 } else {
						 ntpOffset = SimpleMessageUtilities.getInternetNtpOffset();
					 }
					 packetinfo = new Packet(MessageType.REPLY_NTP, ntpOffset);
					 channel.send(members.get(0),packetinfo);
					 state = LocalState.IDLE;
					 continue;
				case GET_LATENCY:  
					 int numProcesses = this.numWorkers+1;
					 int pingDestination = (myIndexOfTheGroup+1)%numProcesses;
					// localClock.initHLC();
					 while(true) {
						 if(pingDestination == 0) pingDestination = 1;
						 if(pingDestination == myIndexOfTheGroup) break;
						 int numTrials = 1000;
						 while(numTrials-->0) {
							//System.out.println("send to " +pingDestination);
						//	 SimpleMessageUtilities.busyWaitMicros(100);
							 SimpleMessageUtilities.spinWaitMicros(100);
						//	 Thread.sleep(1);
						// 	localClock.timestampLocalEventHLC();
						 	localClock.timestampLocalEvent();
						 	Packet packet = new Packet(MessageType.PING_LATENCY, Instant.now(), myIndexOfTheGroup);
						 	channel.send(SimpleMessageUtilities.getOobMessage(members.get(pingDestination), packet));
						 }
						 pingDestination = (pingDestination+1)%numProcesses;
					 }
					 Thread.sleep(1000);
					 localStatisticsCollector.reportStatistics();
					 channel.send(members.get(0),new Packet(MessageType.COLLECT_LATENCY, localStatisticsCollector));
					 state = LocalState.IDLE;
					 continue;
				case SETUP_NORMAL_RUN:
					 nonLeaderSetup(); 
					 System.out.println("SETUP FOR NORMAL RUN READY: My index is " + Integer.toString(myIndexOfTheGroup));
					 initL = parameters.startTime.toInstant().plusMillis(parameters.duration+5*1000);  
					 lastL = initL.plusMillis(parameters.duration);
					 //System.out.println(initL);
					// System.out.println("vs. now: "+Instant.now());
					 SimpleMessageUtilities.waitUntil(Date.from(initL));
					 initTime  = Instant.now();
					 localClock.timestampLocalEvent();  
					 localTraceCollector.pushLocalTrace(new LocalEvent(EventType.START, localClock, initL));
					 state = LocalState.EXECUTE_NORMAL_RUN;
					 continue;
				case SETUP_LATENCY_RUN:
					 nonLeaderSetup(); 
					 
					 System.out.println("SETUP FOR LATENCY RUN READY: My index is " + Integer.toString(myIndexOfTheGroup));
					 initL = parameters.startTime.toInstant();
					 lastL = initL.plusMillis(parameters.duration);
					  //System.out.println(initL);
					 SimpleMessageUtilities.waitUntil(Date.from(initL)); 
					 initTime  = Instant.now();
					 localClock.timestampLocalEvent();  
					
					 state = LocalState.EXECUTE_LATENCY_RUN;
					 continue;
				case EXECUTE_NORMAL_RUN:
					SimpleMessageUtilities.spinWaitMicros(parameters.timeUnitMicrosec);
				    sendMessage = parameters.nextDouble() <= parameters.unicastProbability;
					if(sendMessage) 
					{
						int destination = parameters.nextDestination();
						if(destination == myIndexOfTheGroup) {
							localClock.timestampLocalEvent();
							localTraceCollector.pushLocalTrace(new LocalEvent(EventType.LOCAL_EVENT, localClock, Instant.now()));
						} else {
							localClock.timestampSendEvent(); 
							Packet packet = new Packet(MessageType.NORMAL_RECEIVE,localClock);
							channel.send(SimpleMessageUtilities.getOobMessage(members.get(destination), packet));
							localTraceCollector.pushLocalTrace(new LocalEvent(EventType.SEND_MESSAGE, localClock, Instant.now()));
							System.out.println("Send to : " +destination);
							localClock.print();
						}
					}
					 elapsedTime =  Duration.between(initTime, Instant.now());
					 if(elapsedTime.toMillis() > parameters.duration) { 
						 state = LocalState.FINISH_NORMAL_RUN;
					 }
					 
					 continue;
				case EXECUTE_LATENCY_RUN:
					 
					SimpleMessageUtilities.spinWaitMicros(parameters.timeUnitMicrosec);
					sendMessage = parameters.nextDouble() <= parameters.unicastProbability;
					if(sendMessage) 
					{
						int destination = parameters.nextDestination();
						if(destination != myIndexOfTheGroup) {
							localClock.timestampSendEvent(); 
						  	Packet packet = new Packet(MessageType.PING_LATENCY, Instant.now(), myIndexOfTheGroup);
							channel.send(SimpleMessageUtilities.getOobMessage(members.get(destination), packet));
						}
					}
			
					 elapsedTime =  Duration.between(initTime, Instant.now());
					
					 if(elapsedTime.toMillis() > parameters.duration) {  
						state = LocalState.FINISH_LATENCY_RUN;
					 }
					 
					continue;
				case FINISH_LATENCY_RUN:
					localStatisticsCollector.reportStatistics();
					channel.send(members.get(0),new Packet(MessageType.COLLECT_LATENCY, localStatisticsCollector));
					state = LocalState.SETUP_NORMAL_RUN; 
				    	continue;
				    	
				case FINISH_NORMAL_RUN :
					localClock.timestampLocalEvent(); 
					localTraceCollector.pushLocalTrace(new LocalEvent(EventType.STOP,localClock, localClock.getL()));
					localTraceCollector.fillHvcTrace(initL, parameters.HvcCollectingPeriod,lastL);
					localTraceCollector.computeHvcSizeOverTime();
					if(parameters.runQuery) {
						localTraceCollector.computeHvcSizeOverEpsilon(parameters.epsilonStart, parameters.epsilonInterval, parameters.epsilonStop);
					}
					Packet packet = new Packet(MessageType.CONFIG_FINISH,localTraceCollector, myIndexOfTheGroup);
					channel.send(members.get(0),packet);
					
					state = LocalState.IDLE; 
					continue;
				case STOP: 
					 
				default:
				}
				break;
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	private void localComputation() throws Exception {
		myIndexOfTheGroup = indexOfMyAddress(channel.getView().getMembers());	
		boolean thisIsTheLeader = myIndexOfTheGroup == 0;
		if(thisIsTheLeader) {
			runLeaderRoutine();
		} else {
			nonLeaderRoutine();
		}		
	}
	
	public static void main(String[] args) throws Exception { 
		   
		for(int i=0; i < args.length; i++) {
			System.out.println(args[i]);
			if("-bind_addr".equals(args[i])) {
	                System.setProperty("jgroups.bind_addr", args[++i]); 
	                continue;
	         }
			if("-external_addr".equals(args[i]) ){
				   System.setProperty("jgroups.external_addr", args[++i]); 
	               continue;
			}
			if("-help".equals(args[i])) {
				System.out.println("-bind_addr [addr] -external_addr [addr]");
			}
			
		} 
		
		System.setProperty("java.net.preferIPv4Stack", "true");
		new SimpleMessagePassing().start();	
	}
	
	Timestamp localClock; 
	
	JChannel channel;
	String userName=System.getProperty("user.name", "n/a");
	
	private int numWorkers;
	private ArrayList<Double> globalNtpOffset;
		
	private int myIndexOfTheGroup; 
	private LocalState state; 
	private java.util.List<org.jgroups.Address> members;
	
	private LeaderTraceCollector leaderTraceCollector;
	private LocalTraceCollector localTraceCollector;
	
	private LocalStatisticsCollector localStatisticsCollector;
	//parameters contain global variables and local variables with parameters from the leader.
	private RunningParameters parameters; 

}
