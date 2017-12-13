import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View; 

import java.io.*;
import java.util.Date;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask; 


public class SimpleMessagePassing extends ReceiverAdapter {
	
	private void start() throws Exception {
		channel=new JChannel().setReceiver(this);
       // channel=new JChannel(); // use the default config, udp.xml
        channel.connect("Cluster");
        localComputation();
        channel.close();
    }
	
	public void viewAccepted(View new_view) {
		//System.out.println("** view: " + new_view);
	}
	
	public void receive(Message msg) {
		try {
			Packet receivedPacket = (Packet) (msg.getObject());
			switch(receivedPacket.getMessageType()) {
				case NORMAL_RECEIVE: 
					if(state==LocalState.EXECUTE) {
						Timestamp ts = receivedPacket.getLocalTimestamp();
						localClock.timestampReceiveEvent(ts);
						localClock.timestampReceiveEventHLC(ts);
						localTraceCollector.pushLocalTrace(new LocalEvent(EventType.RECEIVE_MESSAGE,localClock,System.currentTimeMillis()));
						System.out.print("Received: ");
						localClock.print();
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
					leaderTraceCollector.addTraceFrom(receivedPacket.getAllLocalEvents(),receivedPacket.getIndexFrom());
					if ( leaderTraceCollector.hasReceivedFromAllMembers() ) {
						state = LocalState.IDLE;
						leaderTraceCollector.printGlobalTrace();
					} 
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
		this.numberOfMembers = members.size();
		this.randomSource = new Random();
		this.localClock = new HybridVectorClock(numberOfMembers, myIndexOfTheGroup,parameters.epsilon);
	}
	private void leaderSetup() {
		setup();
		leaderTraceCollector = new LeaderTraceCollector(numberOfMembers);
	}
	private void nonLeaderSetup() {
		setup();
		localTraceCollector = new LocalTraceCollector();
	}
	private void printInstruction() {
		System.out.println("usage: start "
						+ "[int duration:secs] "
						+ "[int timeunit:ms] "
						+ "[unicast_probability:0-1] "
						+ "[long hvc_collecting_peroid:ms] "
						+ "[long epsilon:ms]");
		System.out.println("usage2: exit");
	}
	private boolean correctFormat(String[] cmd) {
		
		boolean lengthEqual6 = cmd.length ==6;
		if(!lengthEqual6) { 
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
		//add 5 secs
		Date startTimeMessage = new Date(System.currentTimeMillis()+ 5*1000);
		long period = Long.parseLong(cmd[4]);
		long epsilon = Long.parseLong(cmd[5]);
		Packet packet = new Packet(MessageType.CONFIG_START,
							new RunningParameters( 
								unicastProbabilityMessage,
								timeunitMessage,
								durationMessage,
								startTimeMessage,
								period,
								epsilon) 
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
		
	private void waitUntilReceivedAllLocalStates() throws Exception {
		while(state == LocalState.SETUP ) {
			Thread.sleep(1000);
			System.out.print(".");
		} 
	}
	
	private void runLeaderRoutine() throws Exception {
		
		while(true) { 
			state = LocalState.IDLE;
			Thread.sleep(100); 
			System.out.print("> ");
			BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
			String line =in.readLine().toLowerCase();
			if(line.startsWith("start")) {
				String[] command = line.split(" ");
				if(!correctFormat(command)) {
					printInstruction();
					continue;
				}
				broadcastCommand(command);
				while(state!=LocalState.SETUP);
				leaderSetup();		
				waitUntilReceivedAllLocalStates();
				//TODO run a data collection output here
				System.out.println("The execution has been completed. YAY.");
			}
			else if (line.startsWith("quit") || line.startsWith("exit")) {
				Packet packet = new Packet(MessageType.CONFIG_STOP);
				channel.send(null,packet);
				return;
			}
			else {
				printInstruction();
			} 
		}
	}
	private void nonLeaderRoutine() throws Exception {
		 //the following loop is for non-leaders
		state = LocalState.IDLE;
		long initTime = 0;
		long elapsedTime = 0L; 
		long initL = 0;
		long lastL = 0;
		//local state may be changed upon receiving a message
		while(true) {
			switch(state) {
			case IDLE:  
				Thread.sleep(1000); 
				System.out.print(".");
				//waiting for leader's command to run or to stop the programs.
				continue;
			case SETUP: 
				 nonLeaderSetup(); 
				 System.out.println("SETUP READY: My index is " + Integer.toString(myIndexOfTheGroup));
				 initL = parameters.startTime.getTime();
				 lastL = initL+parameters.duration;
				 waitUntil(parameters.startTime);
				 initTime  = System.currentTimeMillis();
				 localClock.timestampLocalEvent();
				 localClock.timestampLocalEventHLC();
				 //initL = localClock.getL();
				 localTraceCollector.pushLocalTrace(new LocalEvent(EventType.START, localClock, initL));
				 state = LocalState.EXECUTE;
				 continue;
			case EXECUTE: 
				Thread.sleep(parameters.timeUnitMillisec);
				boolean sendMessage = randomSource.nextDouble() <= parameters.unicastProbability;
				if(sendMessage) {
					int destination = randomSource.nextInt(numberOfMembers-1)+1;
					localClock.timestampSendEvent();
					localClock.timestampSendEventHLC();
					Packet packet = new Packet(MessageType.NORMAL_RECEIVE,localClock);
					channel.send(members.get(destination),packet);
					localTraceCollector.pushLocalTrace(new LocalEvent(EventType.SEND_MESSAGE, localClock, System.currentTimeMillis()));
					//System.out.print("Send to "+  Integer.toString(destination)+ ": ");
					//localClock.print();
				}
				 elapsedTime =  System.currentTimeMillis() - initTime;
				 if(elapsedTime > parameters.duration) state = LocalState.FINISH;
				 continue;
			case FINISH: 
				localClock.timestampLocalEvent();
				localClock.timestampLocalEventHLC(); 
				localTraceCollector.pushLocalTrace(new LocalEvent(EventType.STOP,localClock, localClock.getL()));
				localTraceCollector.fillHvcTrace(initL, parameters.HvcCollectingPeriod,lastL);
				localTraceCollector.computeHvcSizeOverTime();
				Packet packet = new Packet(MessageType.CONFIG_FINISH,localTraceCollector, myIndexOfTheGroup);
				channel.send(members.get(0),packet);
				state = LocalState.IDLE;
				continue;
			case STOP: 
				 
			default:
			}
			break;
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
		System.setProperty("java.net.preferIPv4Stack", "true");
		new SimpleMessagePassing().start();	
	}
	
	Timestamp localClock;
	JChannel channel;
	String userName=System.getProperty("user.name", "n/a");
	
	private int myIndexOfTheGroup;
	private int numberOfMembers; 
	
	private java.util.List<org.jgroups.Address> members;
	
	private LeaderTraceCollector leaderTraceCollector;
	private LocalTraceCollector localTraceCollector;
	
	private RunningParameters parameters;

	private Random randomSource; 
	private LocalState state;
}
