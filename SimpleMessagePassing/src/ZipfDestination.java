import java.util.ArrayList;

public class ZipfDestination implements RandomDestinationGenerator {
 
	@Override
	public void setup(ArrayList<String> configuration) { 
		
		//configuration = {myRandomSeed, numberOfMembers, double skew}
		if(configuration.size()!=3) System.out.println("Warning wrong configuration for Zipf");
		zipf = new ZipfGenerator(Long.parseLong(configuration.get(0)),Integer.parseInt(configuration.get(1))-1, Double.parseDouble(configuration.get(2)));
	}
	@Override
	public int nextDistination() { 
		return zipf.next();
	}
	
	private ZipfGenerator zipf;
}
