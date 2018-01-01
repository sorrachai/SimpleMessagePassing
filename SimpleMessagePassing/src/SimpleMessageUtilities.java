import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.time.Instant;
import java.util.ArrayList;

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
   
	 public static void busyWaitMicros(long micros){
		//thanks to: http://www.rationaljava.com/2015/10/measuring-microsecond-in-java.html
        //NanoTimer is a precise timer.
		long waitUntil = System.nanoTime() + (micros * 1_000);
        while(waitUntil > System.nanoTime());
    }
	 
	public static void writeHistogramToFile(String filename, int [] frequency, int totalFrequency) {
				DecimalFormat percentage = new DecimalFormat("00.00");
				FileWriter file;
				try {
					file = new FileWriter("./" + filename,false);
					for(int i=1;i<frequency.length;i++) {
					file.write(Integer.toString(i)+" "+percentage.format(100.0 *(frequency[i] / (double)totalFrequency)));
					file.write(System.getProperty( "line.separator" ));
				}
				file.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			
	}
	public static double average(ArrayList<Double> in) {
		double sum = 0;
		for(double d : in) {
			sum+= Math.abs(d);
		}
		return sum/in.size();
	}
	public static double getInternetNtpOffset() {
		return runCommandReturnDouble("ntpq -c kerninfo | awk \'/offset/ { print $3 }\'"); 
	}
	
	public static double getAmazonNtpOffset() {
		return runCommandReturnDouble("chronyc tracking | awk \'/RMS offset/ { print $4 }\'");
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
			System.out.println(output);
			return Double.parseDouble(output);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			System.out.println("Oops, cannot get offset");
		}
		return 555555;
	}
}
