 
import java.util.ArrayList;

public interface RandomDestinationGenerator{
	 // return between 1 and numberOfNodes inclusive.
	 public void setup(ArrayList<String> configuration);
	 public int nextDistination();
}
