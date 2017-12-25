import java.util.ArrayList;
import java.util.Random;

public class UniformDestination implements RandomDestinationGenerator{

 
	@Override
	public void setup(ArrayList<String> configuration) {
		
		//configuration = {myRandomSeed, numberOfMembers}
		if(configuration.size() != 2) System.out.println("Warning: invalid configuration for uniform");
		numProcesses = Integer.parseInt(configuration.get(1));
		source = new Random(Long.parseLong(configuration.get(0)));
	}

	@Override
	public int nextDistination() {
		return source.nextInt(numProcesses-1)+1;
	}
	private Random source; 
	private int numProcesses;
}
