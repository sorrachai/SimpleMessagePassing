import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.time.Instant;

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
}
