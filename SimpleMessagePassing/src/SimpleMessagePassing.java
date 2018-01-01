import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View; 

import java.io.*;
import java.util.ArrayList;
import java.util.Date; 
import java.util.Timer;
import java.util.TimerTask; 

import java.time.Instant;
import java.time.Duration;

public class SimpleMessagePassing extends ReceiverAdapter {
	
	@SuppressWarnings("resource")
	private void start() throws Exception {
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
				case NORMAL_RECEIVE: 
					if(state==LocalState.EXECUTE) {
						Timestamp ts = receivedPacket.getLocalTimestamp();
						synchronized(localClock) {
							localClock.timestampReceiveEvent(ts);
							localClock.timestampReceiveEventHLC(ts);
						}
						localTraceCollector.pushLocalTrace(new LocalEvent(EventType.RECEIVE_MESSAGE,localClock,Instant.now()));
						//System.out.print("Received: ");
						//localClock.print();
					}
				break;
				case CONFIG_START:
					parameters = new RunningParameters(receivedPacket.getRunningParameter());
					
					state = LocalState.SETUP;
				break;
				case CONFIG_STOP:
					state = LocalState.STOP;
				break;
				case CONFIG_FINISH: 
					System.out.println("Received CONFIG_FINISH");
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
						leaderTraceCollector.printTotalNumSentMessages();
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
					System.out.println(".");
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
			System.out.println(addr + " vs. " + myAddress);
			if(addr.toString()!=myAddress.toString()) {
				i++;
			}
			else {
				return i;
			}
		}
		return i;
	}
	public static boolean isNumeric(String str)  {  
		try  {  
			Double.parseDouble(str);  
		}  
		catch(NumberFormatException nfe)  {  
			return false;  
		}  
		return true;  
	}
	
	
	private void waitUntil(Date date) {
		//https://www.java-forums.org/new-java/11785-sleep-until-certian-time-day.html
        final Object o = new Object();
        TimerTask tt = new TimerTask() {
            public void run() {
                synchronized (o) {
                    o.notify();
                }
            }
        };
        Timer t = new Timer();
        t.schedule(tt, date);
        synchronized(o) {
            try {
                o.wait();
            } catch (InterruptedException ie) {}
        }
        t.cancel();
        t.purge();
    }
	private void setup(){
		this.members = channel.getView().getMembers();  
		this.myIndexOfTheGroup = indexOfMyAddress(members); 
	 	parameters.setRandom(myIndexOfTheGroup);
		this.localClock = new HybridVectorClock(parameters.numberOfMembers, myIndexOfTheGroup,parameters.globalEpsilon);
	}
	private void leaderSetup() {
		setup();
		leaderTraceCollector = new LeaderTraceCollector(parameters.numberOfMembers,parameters.startTime.toInstant());
	}
	private void nonLeaderSetup() {
		setup();
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
						+ "[string query: no || yes={epsilon_start:epsilon_interval:epsilon_stop}");
		System.out.println("usage2: info num_nodes || ntp_internet (ms) || ntp_amazon (s)");
		System.out.println("usage3: exit");
	}
	private boolean correctFormat(String[] cmd) {
		
		boolean lengthEqual8 = cmd.length == 8;
		if(!lengthEqual8) { 
			return false;
		}
		boolean firstisint = isInteger(cmd[1]);
		boolean secondisint = isInteger(cmd[2]);
		boolean thirdisreal = isNumeric(cmd[3]);
		boolean fourthislong = isLong(cmd[4]);
		boolean fifthislong = isLong(cmd[5]);
		
		if( firstisint && secondisint && thirdisreal && fourthislong && fifthislong)  {
			double unicastProbabilityMessage = Double.parseDouble(cmd[3]);
			if(unicastProbabilityMessage <= 0 || unicastProbabilityMessage >1)  {
				System.out.println("Need unicast_probability between 0 and 1");
				return false;
			}
			return true;
		}
		return false;
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
								queryString
								) 
		);
	    channel.send(null,packet);
	    
	    System.out.println("Multicast messages have been sent.");
		System.out.println("With the following parameters:");
		System.out.println("StartTime = "+startTimeMessage);
		System.out.println("Duration = "+ cmd[1]);
		System.out.println("Time Unit = "+cmd[2]);
		System.out.println("UnicastProbability = "+cmd[3]);
		System.out.println("Period = "+cmd[4]);
		System.out.println("Epsilon = " + cmd[5]);
		System.out.println("--");
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
				while(state!=LocalState.SETUP);
				leaderSetup();		
				waitUntilReceivedAllLocalStates(LocalState.SETUP); 
				System.out.println("The execution has been completed.");
			}
			else if(line.startsWith("info")) {
				
				//System.out.println("usage2: info num_nodes || ntp_internet || ntp_amazon");
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
				if(command[1].startsWith("ntp_internet")) {
					packet = new Packet(MessageType.REQUEST_INTERNET_NTP);
					//channel.send(SimpleMessageUtilities.getOobMessage(null, packet));
					channel.send(null,packet);
					state = LocalState.GET_INTERNET_NTP;
				
				}
				else if(command[1].startsWith("ntp_amazon")) {
					packet = new Packet(MessageType.REQUEST_AMAZON_NTP);
					channel.send(null,packet);
					state = LocalState.GET_AMAZON_NTP;
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
			//local state may be changed upon receiving a message
			while(true) {
				switch(state) {
				case IDLE:  
					Thread.sleep(1000); 
					System.out.print(".");
					/*this.members = channel.getView().getMembers();  
					this.myIndexOfTheGroup = indexOfMyAddress(members); 
					if(myIndexOfTheGroup==1) {
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
				 
				case SETUP: 
					 nonLeaderSetup(); 
					 System.out.println("SETUP READY: My index is " + Integer.toString(myIndexOfTheGroup));
					 initL = parameters.startTime.toInstant();
					 lastL = initL.plusSeconds(parameters.duration);
					 waitUntil(parameters.startTime);
					 initTime  = Instant.now();
					 localClock.timestampLocalEvent();
					 localClock.timestampLocalEventHLC();
					 //initL = localClock.getL();
					 localTraceCollector.pushLocalTrace(new LocalEvent(EventType.START, localClock, initL));
					 state = LocalState.EXECUTE;
					 continue;
				case EXECUTE: 
					SimpleMessageUtilities.busyWaitMicros(parameters.timeUnitMicrosec);
					boolean sendMessage = parameters.nextDouble() <= parameters.unicastProbability;
					if(sendMessage) {
						int destination = parameters.nextDestination();
						localClock.timestampSendEvent();
						localClock.timestampSendEventHLC();
						Packet packet = new Packet(MessageType.NORMAL_RECEIVE,localClock);
					//	channel.send(members.get(destination),packet);
						channel.send(SimpleMessageUtilities.getOobMessage(members.get(destination), packet));
						localTraceCollector.pushLocalTrace(new LocalEvent(EventType.SEND_MESSAGE, localClock, Instant.now()));
					
						//System.out.print("Send to "+  Integer.toString(destination)+ ": ");
						//localClock.print();
					}
					 elapsedTime =  Duration.between(initTime, Instant.now());//Instant.now() - initTime;
					 if(elapsedTime.toMillis() > parameters.duration) state = LocalState.FINISH;
					 continue;
				case FINISH: 
					localClock.timestampLocalEvent();
					localClock.timestampLocalEventHLC(); 
					localTraceCollector.pushLocalTrace(new LocalEvent(EventType.STOP,localClock, localClock.getL()));
					localTraceCollector.fillHvcTrace(initL, parameters.HvcCollectingPeriod,lastL);
					localTraceCollector.computeHvcSizeOverTime();
					if(parameters.runQuery) {
						localTraceCollector.computeHvcSizeOverEpsilon(parameters.epsilonStart, parameters.epsilonInterval, parameters.epsilonStop);
					}
					Packet packet = new Packet(MessageType.CONFIG_FINISH,localTraceCollector, myIndexOfTheGroup);
				//	System.out.println("Sending back");
					Thread.sleep(3000); 
					channel.send(members.get(0),packet);
				//	System.out.println("DONE");
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
	
	public boolean isInteger( String input ) {
	    try {
	        Integer.parseInt( input );
	        return true;
	    }
	    catch( Exception e ) {
	        return false;
	    }
	}
	
	public boolean isLong( String input ) {
	    try {
	        Long.parseLong( input );
	        return true;
	    }
	    catch( Exception e ) {
	        return false;
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
	
	//parameters contain global variables and local variables with parameters from the leader.
	private RunningParameters parameters; 

}
