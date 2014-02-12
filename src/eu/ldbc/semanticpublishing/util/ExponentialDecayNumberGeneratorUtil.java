package eu.ldbc.semanticpublishing.util;

import java.util.NoSuchElementException;

/**
 * Exponential decay generator, produces values in the exponential decay form of y = C*exp(-k*t), k > 0
 * Where :
 *    C is decayLimit, or maximum number decay starts from
 *    k is decayRate - values from [0.01..1.0] used to define the slope of the decay
 *    t - variable which in this case is the time
 *    
 * When no more elements can be generated, or threshold is reached a NoSuchElementException exception is thrown
 */
public class ExponentialDecayNumberGeneratorUtil {
	private long step = 0;
	private Long next = null;
	
	private long decayLimit = 1;
	private double decayRate = 0.1;
	private double decayThresholdPercent = 0.05;
	
	public ExponentialDecayNumberGeneratorUtil(long decayLimit, double decayRate, double decayThresholdPercent) {
		this.next = decayLimit;
		this.decayLimit = decayLimit;
		this.decayRate = decayRate;
		this.decayThresholdPercent = decayThresholdPercent;
	}
	
	public synchronized Long generateNext() throws NoSuchElementException {
		long result = (long) (decayLimit * Math.exp(-decayRate*step));
		next = result;
		step++;
		
		if (thresholdReached()) {
			next = null;
			throw new NoSuchElementException("No more values to generate (or the threshold of " + decayLimit * decayThresholdPercent + " was reached)");
		}
		
		return result;		
	}

	public boolean hasNext() {
		return next != null;
	}
	
	private double calculateThreshold() {
		return ((double)next / (double)decayLimit);
	}
	
	public boolean thresholdReached() {
		return calculateThreshold() <= decayThresholdPercent;
	}
}
