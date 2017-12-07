import java.io.Serializable;
import java.util.Date;

public class RunningParameters implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 2862398035679017705L; 
	
	public final double unicastProbability; 
	public final long timeUnitMillisec;
	public final long duration;
	public final Date startTime;
	
	public RunningParameters( double unicastProbability,
							  int timeUnitMillisec,
							  long duration,
							  Date startTime) {
		this.unicastProbability = unicastProbability;
		this.timeUnitMillisec = timeUnitMillisec;
		this.duration = duration;
		this.startTime = startTime;
  }
  public RunningParameters(RunningParameters p) {
	   this.unicastProbability = p.unicastProbability;
	   this.timeUnitMillisec = p.timeUnitMillisec;
	   this.duration = p.duration;
	   this.startTime = p.startTime;
  }
}
