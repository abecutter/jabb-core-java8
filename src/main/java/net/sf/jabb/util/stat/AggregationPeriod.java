/**
 * 
 */
package net.sf.jabb.util.stat;

import java.io.Serializable;
import java.time.Duration;
import java.time.ZoneId;

import net.sf.jabb.util.time.TimeZoneUtility;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * A period of time qualified by a quantity and a unit, with time zone.
 * @author James Hu
 *
 */
public class AggregationPeriod implements Serializable, Comparable<AggregationPeriod>{
	private static final long serialVersionUID = -492234416175692243L;
	
	protected int amount;
	protected AggregationPeriodUnit unit;
	protected ZoneId zone;
	protected transient String codeName;			// as a cache
	
	public AggregationPeriod(){
		
	}
	
	/**
	 * Constructor using UTC time zone
	 * @param amount	the amount
	 * @param unit		the unit
	 */
	public AggregationPeriod(int amount, AggregationPeriodUnit unit){
		this(amount, unit, TimeZoneUtility.UTC);
	}
	
	/**
	 * Constructor
	 * @param amount	the amount
	 * @param unit		the unit
	 * @param zone		the time zone
	 */
	public AggregationPeriod(int amount, AggregationPeriodUnit unit, ZoneId zone){
		this.amount = amount;
		this.unit = unit;
		this.zone = zone;
	}
	
	/**
	 * Return a copy of this with a new amount
	 * @param newAmount	the new amount
	 * @return	a new instance with the new amount and the same unit as this
	 */
	public AggregationPeriod withAmount(int newAmount){
		return new AggregationPeriod(newAmount, unit, zone);
	}
	
	/**
	 * Return a copy of this
	 * @return	the new instance with the same amount and unit
	 */
	public AggregationPeriod copy(){
		return new AggregationPeriod(amount, unit, zone);
	}
	
	/**
	 * Parse strings like '1 hour', '2 days', '3 Years', '12 minute', '5day' into AggregationPeriod.
	 * Short formats like '1H', '2 D', '3y' are also supported.
	 * @param amountAndUnit	the string to be parsed
	 * @param zone  the time zone
	 * @return	the AggregationPeriod instance with specified amount and unit
	 */
	public static AggregationPeriod parse(String amountAndUnit, ZoneId zone){
		String trimed = amountAndUnit.trim();
		String allExceptLast = trimed.substring(0, trimed.length() - 1);
		if (StringUtils.isNumericSpace(allExceptLast)){ // short format
			int amount = Integer.parseInt(allExceptLast.trim());
			AggregationPeriodUnit unit = AggregationPeriodUnit.parse(trimed.charAt(trimed.length() - 1));
			return new AggregationPeriod(amount, unit, zone);
		}else{
			int firstNonNumeric = indexOfFirstNonNumeric(trimed);
			int amount = Integer.valueOf(trimed.substring(0, firstNonNumeric));
			AggregationPeriodUnit unit = AggregationPeriodUnit.parse(trimed.substring(firstNonNumeric).trim());
			return new AggregationPeriod(amount, unit, zone);
		}
	}
	
	/**
	 * Parse strings like '1 hour', '2 days', '3 Years', '12 minute' into AggregationPeriod.
	 * Short formats like '1H', '2 D', '3y' are also supported.
	 * Short formats with time zone prefix like 'l70P5A1H', '12tdkg2D', 'g893BA3y' are also supported.
	 * @param zoneAndAmountAndUnit	the full string
	 * @return	the AggregationPeriod instance with time zone, amount and unit specified in the input string
	 */
	public static AggregationPeriod parse(String zoneAndAmountAndUnit){
		String trimed = zoneAndAmountAndUnit.trim();
		String allExceptLast = trimed.substring(0, trimed.length() - 1);
		if (StringUtils.isNumericSpace(allExceptLast)){ // short format without time zone
			int amount = Integer.parseInt(allExceptLast.trim());
			AggregationPeriodUnit unit = AggregationPeriodUnit.parse(trimed.charAt(trimed.length() - 1));
			return new AggregationPeriod(amount, unit, TimeZoneUtility.UTC);
		}else{
			String[] splited = StringUtils.split(trimed);
			if (splited.length == 2 && StringUtils.isNumeric(splited[0])){	// long format without time zone
				int amount = Integer.valueOf(splited[0]);
				AggregationPeriodUnit unit = AggregationPeriodUnit.parse(splited[1]);
				return new AggregationPeriod(amount, unit, TimeZoneUtility.UTC);
			}else if (splited.length == 3){
				ZoneId zone = TimeZoneUtility.toZoneId(splited[0]);
				return parse(splited[1] + " " + splited[2], zone);
			}else if (trimed.length() >= TimeZoneUtility.SHORTENED_ZONE_ID_MIN_LENGTH +2){
				int firstNonAlpha = indexOfFirstNonAlpha(trimed);
				ZoneId zone = TimeZoneUtility.toZoneId(trimed.substring(0, firstNonAlpha));
				return parse(trimed.substring(firstNonAlpha), zone);
			}else{
				throw new IllegalArgumentException("Not in valid format: " + zoneAndAmountAndUnit);
			}
		}
	}
	protected static int indexOfFirstNonAlpha(String s){
		return indexOfFirstNonAlpha(s, 0);
	}
	protected static int indexOfFirstNonAlpha(String s, int startIndex){
		if (s == null || s.length() == 0){
			return 0;
		}

		int i = startIndex;
		for (; i < s.length(); i ++){
			if (!Character.isAlphabetic(s.charAt(i))){
				break;
			}
		}
		return i;
	}

	protected static int indexOfFirstNonNumeric(String s){
		return indexOfFirstNonNumeric(s, 0);
	}
	protected static int indexOfFirstNonNumeric(String s, int startIndex){
		if (s == null || s.length() == 0){
			return 0;
		}

		int i = startIndex;
		for (; i < s.length(); i ++){
			char c = s.charAt(i);
			if (c < '0' || c > '9'){
				break;
			}
		}
		return i;
	}

	@Override
	public boolean equals(Object o){
		if (o == this){
			return true;
		}
		
		if (o != null && o instanceof AggregationPeriod){
			AggregationPeriod that = (AggregationPeriod)o;
			return new EqualsBuilder()
				.append(this.amount, that.amount)
				.append(this.unit, that.unit)
				.append(this.zone, that.zone)
				.isEquals();
		}else{
			return false;
		}
	}
	
	@Override
	public int hashCode(){
		return amount * 7099991 + amount + unit.hashCode() * 738713 + zone.hashCode()*11;
	}

	/**
	 * Determine if the statistics for this period can be used to generate the statistics of an upper level period.
	 * @param upperLevel	the upper level period
	 * @return	true if aggregation is supported, false if not.
	 */
	public boolean canBeAggregatedTo(AggregationPeriod upperLevel){
		return (upperLevel.getDuration().toMillis() <= 1000L * 60 * 15
				|| this.getDuration().toMillis() <= 1000L * 60 * 15
				|| this.zone.normalized().equals(upperLevel.zone.normalized()))
				&& unit.canSupportAggregation(amount, upperLevel.unit, upperLevel.amount);
	}
	
	@Override
	public String toString(){
		return (zone == null ? "null" : zone.getId()) + "(" + String.valueOf(amount) + " " + unit.toString() + ")";
	}
	
	@JsonIgnore
	public String getCodeName(){
		if (codeName == null){
			codeName = getCodeName(amount, unit, zone);
		}
		return codeName;
	}
	
	static public String getCodeName(int amount, AggregationPeriodUnit unit, ZoneId zone){
		return TimeZoneUtility.toShortenedId(zone) + String.valueOf(amount) + unit.getCode();
	}
	
	static public String getCodeName(int amount, AggregationPeriodUnit unit){
		return getCodeName(amount, unit, TimeZoneUtility.UTC);
	}
	
	@JsonIgnore
	public Duration getDuration(){
		return unit.getTemporalUnit().getDuration().multipliedBy(amount);
	}
	
	@Override
	public int compareTo(AggregationPeriod that) {
		if (that == null){
			return 1;
		}else{
			return new CompareToBuilder()
				.append(this.getDuration(), that.getDuration())
				.append(this.unit.getTemporalUnit().getDuration(), that.unit.getTemporalUnit().getDuration())
				.append(this.zone.getId(), that.zone.getId())
				.toComparison();
		}
	}


	public int getAmount() {
		return amount;
	}

	public void setAmount(int amount) {
		this.amount = amount;
	}

	public AggregationPeriodUnit getUnit() {
		return unit;
	}

	public void setUnit(AggregationPeriodUnit unit) {
		this.unit = unit;
	}

	public ZoneId getZone() {
		return zone;
	}

	public void setZone(ZoneId zone) {
		this.zone = zone;
	}

}
