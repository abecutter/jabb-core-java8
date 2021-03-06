/**
 * 
 */
package net.sf.jabb.util.parallel;

/**
 * Statistics of a dispatching
 * @author James Hu
 *
 * @param <L>	type of the load
 * @param <P>	type of the processor
 */
public class DispatchingStatistics<L, P>{
	protected L load;
	protected P processor;
	protected Long duration;
	protected boolean successful;
	protected Integer exceptionType;
	
	public DispatchingStatistics(){
		
	}
	
	public DispatchingStatistics(L load, P processor, Long duration, boolean successful, Integer exceptionType){
		this.load = load;
		this.processor = processor;
		this.duration = duration;
		this.successful = successful;
		this.exceptionType = exceptionType;
	}

	public L getLoad() {
		return load;
	}

	public void setLoad(L load) {
		this.load = load;
	}

	public P getProcessor() {
		return processor;
	}

	public void setProcessor(P processor) {
		this.processor = processor;
	}

	public Long getDuration() {
		return duration;
	}

	public void setDuration(Long duration) {
		this.duration = duration;
	}

	public boolean isSuccessful() {
		return successful;
	}

	public void setSuccessful(boolean successful) {
		this.successful = successful;
	}

	public Integer getExceptionType() {
		return exceptionType;
	}

	public void setExceptionType(Integer exceptionType) {
		this.exceptionType = exceptionType;
	}
	
	
	
}
