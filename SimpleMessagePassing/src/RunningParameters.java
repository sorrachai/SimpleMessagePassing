import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;

public class RunningParameters implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 2862398035679017705L; 
	
	public final double unicastProbability; 
	public final long timeUnitMicrosec;
	public final long duration;
	public final Date startTime;
	public final long HvcCollectingPeriod;
	public final long globalEpsilon;
	private final String destinationDistributionString;
	private final String queryString;
	public final int numberOfMembers;
	public final long epsilonInterval; 
	public final long epsilonStart;
	public final long epsilonStop;
	public final boolean runQuery;
	private final long initialRandomSeed;
	
	private RandomDestinationGenerator randomDestination;
	private long myRandomSeed;
	private Random randomSource;
 
	public RunningParameters( int numberOfMembers,
							  long initialRandomSeed,
							  double unicastProbability,
							  int timeUnitMicrosec,
							  long duration,
							  Date startTime,
							  long HVCCollectingPeriod,
							  long epsilon,
							  String destinationDistributionString,
							  String queryString
							  ) {
		this.numberOfMembers = numberOfMembers;
		this.unicastProbability = unicastProbability;
		this.timeUnitMicrosec = timeUnitMicrosec;
		this.duration = duration;
		this.startTime = startTime;
		this.HvcCollectingPeriod = HVCCollectingPeriod;
		this.globalEpsilon = epsilon;
		this.destinationDistributionString = destinationDistributionString;
		this.queryString = queryString;
		epsilonInterval = epsilonStart = epsilonStop = 0;
		runQuery = false;
		this.initialRandomSeed = initialRandomSeed;
  }
  public RunningParameters(RunningParameters p) {
	   this.numberOfMembers = p.numberOfMembers;
	   this.unicastProbability = p.unicastProbability;
	   this.timeUnitMicrosec = p.timeUnitMicrosec;
	   this.duration = p.duration;
	   this.startTime = p.startTime;
	   this.HvcCollectingPeriod = p.HvcCollectingPeriod;
	   this.globalEpsilon = p.globalEpsilon;
	   this.destinationDistributionString = p.destinationDistributionString;
	   this.queryString = p.queryString; 
	   this.initialRandomSeed = p.initialRandomSeed;
	   
		if(queryString.startsWith("yes")) {
			this.runQuery = true;
			String lineParameters = queryString.split("=")[1];
			String epsilonParameters[] = lineParameters.split(":");
			epsilonStart = Long.parseLong(epsilonParameters[0]);
			epsilonInterval = Long.parseLong(epsilonParameters[1]);
			epsilonStop = Long.parseLong(epsilonParameters[2]);
		} else {
			this.runQuery = false;
			epsilonInterval = epsilonStart = epsilonStop = 0;
		}
  }
  public void setRandom(int myIndexOfTheGroup) {
	   myRandomSeed = initialRandomSeed +myIndexOfTheGroup;
	   ArrayList<String> t = new ArrayList<>();
	   //t = {myRandomSeed, numberOfMembers}
	    t.add(Long.toString(myRandomSeed));
	    t.add(Integer.toString(numberOfMembers));
		if(destinationDistributionString.startsWith("uniform")) {	
			this.randomDestination = new UniformDestination();
			this.randomDestination.setup(t);
		} else if (destinationDistributionString.startsWith("zipf")) {
			this.randomDestination = new ZipfDestination();
			t.add(destinationDistributionString.split("=")[1]);
			//t = {myRandomSeed, numberOfMembers, double skew}
			this.randomDestination.setup(t);
		} else {
			System.out.println("Warning: dunno distribution. Use uniform as default.");
			this.randomDestination = new UniformDestination();
			this.randomDestination.setup(t);
		}
		
		randomSource = new Random(myRandomSeed);
  }
  public int nextDestination() {
	  return randomDestination.nextDistination();
  }
  public double nextDouble() {
	  return randomSource.nextDouble();
  }
}
