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
  
}
