import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;
import org.jgroups.util.Util;

import com.sun.xml.internal.bind.v2.schemagen.xmlschema.List;
import com.sun.xml.internal.ws.wsdl.writer.document.http.Address;

import java.io.*;  


public class SimpleMessagePassing extends ReceiverAdapter {
	Timestamp localClock;
	JChannel channel;
	String userName=System.getProperty("user.name", "n/a");
	
	private void start() throws Exception {
		channel=new JChannel().setReceiver(this);
       // channel=new JChannel(); // use the default config, udp.xml
        channel.connect("Cluster");
        localClock = new VectorClock(); 
        localComputation();
        channel.close();
    }
	public void viewAccepted(View new_view) {
		System.out.println("** view: " + new_view);
	}
	
	public void receive(Message msg) {
		localClock.timestampReceiveEvent(msg);
		//System.out.println(msg.getSrc() + ": " + ((Timestamp) msg.getObject()));
		System.out.println("Received VC");
		localClock.print();
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
	private void localComputation() {
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
					
				}
				else if(isNumeric(line)) {
					int destination = Integer.parseInt(line);
					localClock.timestampSendEvent();
					channel.send(members.get(destination),localClock);
					localClock.print();
				}
				//line="[" + userName + "] " + line;
				//Message msg=new Message(null, line);
				
				//channel.send(null, localClock);
	            //channel.send(msg);
			} catch(Exception ex) { 
				
			}
	    }
	}
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		System.setProperty("java.net.preferIPv4Stack", "true");
		new SimpleMessagePassing().start();
		
	}
	private int myIndexOfTheGroup = 0;
	private java.util.List<org.jgroups.Address> members;
}
