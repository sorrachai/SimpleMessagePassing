import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;
import org.jgroups.util.Util;

import com.sun.xml.internal.bind.v2.schemagen.xmlschema.List;
import com.sun.xml.internal.ws.wsdl.writer.document.http.Address;

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
		System.out.println("** view: " + new_view);
	}
	
	public void receive(Message msg) {
		Packet receivedPacket = (Packet) (msg.getObject());
		switch(receivedPacket.getMessageType()) {
			case NORMAL_RECEIVE: 
				localClock.timestampReceiveEvent(receivedPacket.getLocalTimestamp());
				//System.out.println(msg.getSrc() + ": " + ((Timestamp) msg.getObject()));
				System.out.print("Received: ");
				localClock.print();
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
				traceCollector.addTraceFrom(receivedPacket.getAllLocalEvents(),receivedPacket.getIndexFrom());
				if ( traceCollector.hasReceivedFromAllMembers() ) {
					/*if (traceCollector.globalTraceIsVerified()) {
							System.out.println("TraceCollection is successful.");
						} else {
							System.out.println("Error: traceCollector.globalTraceIsVerified() is false");
					}*/
					state = LocalState.IDLE;
				} 
			break;
			case IGNORE: 
				System.out.println("Warning: received IGNORE message");
			default:
				
			break;
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
			double d = Double.parseDouble(str);  
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
	
	private void setup() {
		members = channel.getView().getMembers();  
		myIndexOfTheGroup = indexOfMyAddress(members);
		numberOfMembers = members.size();
		localClock.init(numberOfMembers, myIndexOfTheGroup);
		traceCollector = new TraceCollector(myIndexOfTheGroup, numberOfMembers);
		randomSource = new Random();
	}
	private void localComputation() throws Exception {
		/*
	private double unicastProbability; 
	private int timeUnitMillisec;
	private long durations;
	*/	
		state = LocalState.IDLE;
		myIndexOfTheGroup = indexOfMyAddress(channel.getView().getMembers());	
		localClock = new VectorClock(); 
		
		while(myIndexOfTheGroup==0) {
			Thread.sleep(100); 
			System.out.print("> ");
			BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
			String line =in.readLine().toLowerCase();
			if(line.startsWith("quit") || line.startsWith("exit")) {
				Packet packet = new Packet(MessageType.CONFIG_STOP);
				channel.send(null,packet);
				return;
			}
			if(line.startsWith("start")) {
				String[] cmd = line.split(" ");
				boolean lengthEqual4 = cmd.length ==4;
				if(!lengthEqual4) {
					System.out.println("usage: start [int duration:secs] "
						+ "[int timeunit:ms] [unicast_probability:0-1]");
					continue;
				}
				boolean firstisint = isInteger(cmd[1]);
				boolean secondisint = isInteger(cmd[2]);
				boolean thirdisreal = isNumeric(cmd[3]);
				
				if( firstisint && secondisint && thirdisreal) {
					double unicastProbabilityMessage = Double.parseDouble(cmd[3]);
					if(unicastProbabilityMessage <= 0 || unicastProbabilityMessage >1)  {
						System.out.println("Need unicast_probability between 0 and 1");
					} else {
						state = LocalState.SETUP;
						setup();
						int durationMessage = (Integer.parseInt(cmd[1]))*1000;
						int timeunitMessage = Integer.parseInt(cmd[2]);
						//add 10 secs
						Date startTimeMessage = new Date(System.currentTimeMillis()+ 10*1000);
						
						Packet packet = new Packet(MessageType.CONFIG_START,
											new RunningParameters(
												unicastProbabilityMessage,
												timeunitMessage,
												durationMessage,
												startTimeMessage)
						);
						channel.send(null,packet);
						
						System.out.println("Multicast messages have been sent.");
						System.out.println("With the following parameters:");
						System.out.println("StartTime = "+startTimeMessage);
						System.out.println("Duration = "+ cmd[1]);
						System.out.println("Time Unit = "+cmd[2]);
						System.out.println("UnicastProbability = "+cmd[3]);
						System.out.println("--");
						while(state == LocalState.SETUP ) {
							Thread.sleep(1000);
							System.out.print(".");
						}
						//waiting until received all messages and results 
						System.out.println("The execution has been completed.");
					}
				}
				else {
					System.out.println("usage: start [int duration:secs] "
						+ "[int timeunit:ms] [unicast_probability:0-1]");
				}
			} else {
				System.out.println("usage: start [int duration:secs] "
						+ "[int timeunit:ms] [unicast_probability:0-1]");
			}
		}
		
	
		long initTime = 0;
		long elapsedTime = 0L;
		//local state may be changed upon receiving a message
		while(true) {
			switch(state) {
			case IDLE:  
				Thread.sleep(1000); 
				System.out.print(".");
				//waiting for leader's command to run or to stop the programs.
				continue;
			case SETUP: 
				
				 setup(); 
				 System.out.println("SETUP READY: My index is " + Integer.toString(myIndexOfTheGroup));
				 waitUntil(parameters.startTime);
				 initTime  = System.currentTimeMillis();
				 state = LocalState.EXECUTE;
				continue;
			case EXECUTE:
			//	System.out.println(parameters.timeUnitMillisec);
				Thread.sleep(1000);
				boolean sendMessage = randomSource.nextDouble() <= parameters.unicastProbability;
				if(sendMessage) {
					int destination = randomSource.nextInt(numberOfMembers-1)+1;
					localClock.timestampSendEvent();
					Packet packet = new Packet(MessageType.NORMAL_RECEIVE,localClock);
					channel.send(members.get(destination),packet);
					System.out.print("Send to "+  Integer.toString(destination)+ ": ");
					localClock.print();
				}
				 elapsedTime =  System.currentTimeMillis() - initTime;
				 if(elapsedTime > parameters.duration) state = LocalState.FINISH;
				continue;
			case FINISH:
				Packet packet = new Packet(MessageType.CONFIG_FINISH,traceCollector.getAllLocalEvents(), myIndexOfTheGroup);
				channel.send(members.get(0),packet);
				reset();
				state = LocalState.IDLE;
				continue;
			case STOP: 
				 
			default:
			}
			break;
		}
		/*
		BufferedReader in=new BufferedReader(new InputStreamReader(System.in));
	    while(true) {
			try {
				Thread.sleep(1000);           //1000 milliseconds is one second.
				System.out.println("myIndexOfTheGroup = "+Integer.toString(myIndexOfTheGroup));
				
				String line =in.readLine().toLowerCase();
				if(line.startsWith("quit") || line.startsWith("exit"))
					 break;
				if(line.startsWith("init")) {
				
				    members = channel.getView().getMembers();  
					myIndexOfTheGroup = indexOfMyAddress(members);
					localClock.init(members.size(), myIndexOfTheGroup);
					traceCollector.init(myIndexOfTheGroup,members.size());
					
				}
				else if(isNumeric(line)) {
					int destination = Integer.parseInt(line);
					localClock.timestampSendEvent();
					Packet packet = new Packet(MessageType.LOCAL_CLOCK,localClock);
					channel.send(members.get(destination),packet);
					localClock.print();
				}
				//line="[" + userName + "] " + line;
				//Message msg=new Message(null, line);
				
				//channel.send(null, localClock);
	            //channel.send(msg);
			} catch(Exception ex) { 
				
			}  
	    }*/
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
	private void reset() {
		
	}
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		System.setProperty("java.net.preferIPv4Stack", "true");
		new SimpleMessagePassing().start();
		
	}
	
	Timestamp localClock;
	JChannel channel;
	String userName=System.getProperty("user.name", "n/a");
	
	
	private int myIndexOfTheGroup = 0;
	private int numberOfMembers = 0;
	private java.util.List<org.jgroups.Address> members;
	private TraceCollector traceCollector;
	private RunningParameters parameters;

	
	private Random randomSource;
	//private int
	private LocalState state;
}
