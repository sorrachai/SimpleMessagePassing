import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import org.jgroups.Address;
import org.jgroups.Message;

public class SimpleMessageUtilities {

	public static Instant maxInstant(Instant a, Instant b) {
		if(a.isAfter(b)) {
			return a;
		}
		if(b.isAfter(a)) {
			return b;
		}
		return a;
	}
   
	 public static void spinWaitMicros(long micros){
		//thanks to: http://www.rationaljava.com/2015/10/measuring-microsecond-in-java.html
        //NanoTimer is a precise timer.
		long waitUntil = System.nanoTime() + (micros * 1_000);
        while(waitUntil > System.nanoTime()) {
        		java.lang.Thread.onSpinWait();
        }
    }
	 
	public static void writeHistogramToFile(String filename, int [] frequency, int totalFrequency, FileWriter outputLog) {
				DecimalFormat percentage = new DecimalFormat("00.00");
				FileWriter file;
				try {
					file = new FileWriter("./" + filename,false);
					for(int i=1;i<frequency.length;i++) {
					file.write(Integer.toString(i)+" "+percentage.format(100.0 *(frequency[i] / (double)totalFrequency)));
					file.write(System.getProperty( "line.separator" ));
				
					outputLog.write(Integer.toString(i)+" "+percentage.format(100.0 *(frequency[i] / (double)totalFrequency)));
					outputLog.write(System.getProperty( "line.separator" ));
				
				}
				file.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			 
	}
	public static double average(Vector<Double> in) {
		double sum = 0;
		for(double d : in) {
			sum+= Math.abs(d);
		}
		return sum/in.size();
	}
	public static double max(Vector<Double> in) {
		double m = Math.abs(in.get(0));
		
		for(double d : in) {
			m = Math.max(m, Math.abs(d));
		}
		return m;
	}
	public static double average(ArrayList<Long> in) {
		double sum = 0;
		for(double d : in) {
			sum+= Math.abs(d);
		}
		return sum/in.size();
	}
	public static double max(ArrayList<Long> in) {
		double m = Math.abs(in.get(0));
		for(double d : in) {
			m = Math.max(m, Math.abs(d));
		}
		return m;
	}
	public static double getInternetNtpOffset() {
		double result =runCommandReturnDouble("ntpq -c kerninfo | awk \'/offset/ { print $3 }\'");  
		System.out.println(result);
		return result;
	}
	
	public static double getAmazonNtpOffset() {
		double result =runCommandReturnDouble("chronyc tracking | awk \'/Last offset/ { print $4 }\'") *1000;
		System.out.println(result);
		return result;
	}
	public static Message getOobMessage(Address dest, Packet p) {
		//Get out-of-band message which no longer guarantee FIFO delivery
		return new Message(dest,p).setFlag(Message.Flag.OOB,	Message.Flag.DONT_BUNDLE,Message.Flag.NO_RELIABILITY, Message.Flag.NO_TOTAL_ORDER, Message.Flag.NO_FC); //.setFlag(arg0)
	}
	private static double runCommandReturnDouble(String cmd) {
	//credit: https://stackoverflow.com/a/6441483/2959347
		Process p;
		try {	 
			String [] shellCmd = { 
				"/bin/sh",
				"-c",
				cmd
			};
			// System.out.println(cmd);
			 p = Runtime.getRuntime().exec(shellCmd);
			 p.waitFor();
			 BufferedReader buf = new BufferedReader(new InputStreamReader(p.getInputStream()));
			 String line = "";
			 String output = "";
	
			while ((line = buf.readLine()) != null) {
			    output += line + "\n";
			}
		//	System.out.println(output);
			return Double.parseDouble(output);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			System.out.println("Oops, cannot get offset");
		}
		return 555555;
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
	
	public static boolean isInteger( String input ) {
	    try {
	        Integer.parseInt( input );
	        return true;
	    }
	    catch( Exception e ) {
	        return false;
	    }
	}
	
	public static boolean isLong( String input ) {
	    try {
	        Long.parseLong( input );
	        return true;
	    }
	    catch( Exception e ) {
	        return false;
	    }
	}
	
	
	public static void waitUntil(Date date) {
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
}
