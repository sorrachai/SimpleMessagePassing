import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;
import org.jgroups.util.Util;

import java.io.*;



public class SimpleMessagePassing extends ReceiverAdapter {

	JChannel channel;
	String user_name=System.getProperty("user.name", "n/a");
	
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
		System.out.println(msg.getSrc() + ": " + msg.getObject());
	}

	private void localComputation() {
		BufferedReader in=new BufferedReader(new InputStreamReader(System.in));
	    while(true) {
			try {
				Thread.sleep(1000);           //1000 milliseconds is one second.
				String line =in.readLine().toLowerCase();
				if(line.startsWith("quit") || line.startsWith("exit"))
					 break;
				line="[" + user_name + "] " + line;
				Message msg=new Message(null, line);
	            channel.send(msg);
			} catch(Exception ex) { 
				
			}
	    }
	}
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		new SimpleMessagePassing().start();
		
	}

}
