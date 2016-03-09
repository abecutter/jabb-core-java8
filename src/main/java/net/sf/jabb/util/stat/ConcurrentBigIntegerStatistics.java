/**
 * 
 */
package net.sf.jabb.util.stat;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.LongAdder;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Thread-safe statistics holder for high precision use cases.
 * @author James Hu
 *
 */
public class ConcurrentBigIntegerStatistics implements NumberStatistics<BigInteger>, Serializable{
	private static final long serialVersionUID = 5258045107455137348L;
	protected static final BigInteger LONG_MAX_VALUE = BigInteger.valueOf(Long.MAX_VALUE);
	protected static final BigInteger LONG_MIN_VALUE = BigInteger.valueOf(Long.MIN_VALUE);
	
	protected LongAdder count;
	protected BigIntegerAdder sum;
	protected ConcurrentBigIntegerMinMaxHolder bigIntegerMinMax;
	protected ConcurrentLongMinMaxHolder longMinMax;
	
	public ConcurrentBigIntegerStatistics(){
		count = new LongAdder();
		sum = new BigIntegerAdder();
		bigIntegerMinMax = new ConcurrentBigIntegerMinMaxHolder();
		longMinMax = new ConcurrentLongMinMaxHolder();
	}
	
	public ConcurrentBigIntegerStatistics(int sumConcurrencyFactor){
		count = new LongAdder();
		sum = new BigIntegerAdder(sumConcurrencyFactor);
		bigIntegerMinMax = new ConcurrentBigIntegerMinMaxHolder();
		longMinMax = new ConcurrentLongMinMaxHolder();
	}
	
	@Override
	public boolean equals(Object other){
		if (other == this){
			return true;
		}
		if (!(other instanceof NumberStatistics<?>)){
			return false;
		}
		NumberStatistics<?> that = (NumberStatistics<?>) other;
		return new EqualsBuilder()
			.append(this.count, that.getCount())
			.append(this.sum, that.getSum())
			.append(this.getMin(), that.getMin())
			.append(this.getMin(), that.getMax())
			.isEquals();
	}
	
	@Override
	public int hashCode(){
		return new HashCodeBuilder()
				.append(count)
				.append(sum)
				.append(bigIntegerMinMax)
				.append(longMinMax)
				.toHashCode();
	}

	@Override
	public void evaluate(BigInteger x){
		count.increment();
		sum.add(x);
		bigIntegerMinMax.evaluate(x);
	}
	
	@Override
	public void evaluate(int x){
		count.increment();
		sum.add(x);
		longMinMax.evaluate(x);
	}
	
	@Override
	public void evaluate(long x){
		count.increment();
		sum.add(x);
		longMinMax.evaluate(x);
	}
	
	@Override
	public long getCount(){
		return count.sum();
	}
	
	@Override
	public BigInteger getSum(){
		return sum.sum();
	}
	
	@Override
	public BigInteger getMin(){
		BigInteger bigIntegerMin = bigIntegerMinMax.getMin();
		Long longMin = longMinMax.getMinAsLong();
		if (bigIntegerMin == null){
			return longMin == null ? null : BigInteger.valueOf(longMin);
		}else{
			return longMin == null ? bigIntegerMin : bigIntegerMin.max(BigInteger.valueOf(longMin));
		}
	}
	
	@Override
	public BigInteger getMax(){
		BigInteger bigIntegerMax = bigIntegerMinMax.getMax();
		Long longMax = longMinMax.getMaxAsLong();
		if (bigIntegerMax == null){
			return longMax == null ? null : BigInteger.valueOf(longMax);
		}else{
			return longMax == null ? bigIntegerMax : bigIntegerMax.max(BigInteger.valueOf(longMax));
		}
	}
	
	@Override
	public void reset(){
		count.reset();
		sum.reset();
		bigIntegerMinMax.reset();
		longMinMax.reset();
	}
	
	@Override
	public void reset(long newCount, BigInteger newSum, BigInteger newMin, BigInteger newMax){
		count.reset();
		count.add(newCount);
		sum.set(newSum);
		if (newMax.compareTo(LONG_MAX_VALUE) <= 0 && newMin.compareTo(LONG_MIN_VALUE) >= 0){
			longMinMax.reset(newMin.longValue(), newMax.longValue());
			bigIntegerMinMax.reset();
		}else{
			bigIntegerMinMax.reset(newMin, newMax);
			longMinMax.reset();
		}
	}
	
	@Override
	public Double getAvg() {
		BigDecimal avg = getAvg(30);
		return avg == null? null : avg.doubleValue();
	}

	@Override
	public BigDecimal getAvg(int scale) {
		long countValue = count.sum();
		if (countValue > 0){
			if (getMin().equals(getMax())){
				BigDecimal avg = new BigDecimal(getMin());
				avg.setScale(scale);
				return avg;
			}else{
				BigDecimal avg = new BigDecimal(sum.sum());
				return avg.divide(new BigDecimal(countValue), scale, BigDecimal.ROUND_HALF_UP);
			}
		}else{
			return null;
		}
	}

	@Override
	public String toString(){
		return "(" + count.sum() + ", " + sum.sum() + ", " + getMin() + "/" + getMax() + ")";
	}

	@Override
	public void merge(long count, BigInteger sum, BigInteger min, BigInteger max) {
		this.count.add(count);
		if (sum != null){
			this.sum.add(sum);
		}
		if (min != null){
			this.bigIntegerMinMax.evaluate(min);
		}
		if (max != null){
			this.bigIntegerMinMax.evaluate(max);
		}
	}
	
	public void mergeBigInteger(NumberStatistics<? extends BigInteger> other) {
		if (other != null){
			merge(other.getCount(), other.getSum(), other.getMin(), other.getMax());
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void merge(NumberStatistics<? extends Number> other) {
		if (other != null && other.getCount() > 0){
			if (other.getSum() instanceof BigInteger){
				mergeBigInteger((NumberStatistics<? extends BigInteger>)other);
			}else{
				long otherCount = other.getCount();
				if (otherCount > 0){
					count.add(other.getCount());
					sum.add(other.getSum().longValue());
					longMinMax.evaluate(other.getMin().longValue());
					longMinMax.evaluate(other.getMax().longValue());
				}
			}
		}
	}


}
