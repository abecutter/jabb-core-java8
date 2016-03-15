/**
 * 
 */
package net.sf.jabb.util.stat;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * Time period key scheme that is in a hierarchical structure.
 * For any of the key schemes, there is always 0 or 1 lower level scheme, and 0 or n upper level schemes.
 * For any of the keys, the lower level scheme may have more than 1 corresponding keys, 
 * and each of the upper level schemes can have only 1 corresponding key.
 * @author James Hu
 *
 */
public interface HierarchicalAggregationPeriodKeyScheme{
	
	/**
	 * To check if a key is valid
	 * @param key	the key
	 * @return	true if it is a valid key, false if it is not
	 */
	default boolean isValid(String key){
		try{
			getStartTime(key);
			return true;
		}catch(Exception e){
			return false;
		}
	}

	/**
	 * Get the start time (inclusive) of the time period represented by the key.
	 * The key always marks the start time so there is no time zone information needed as argument.
	 * @param key	the time period key
	 * @return	the start time (inclusive) of the time period. It should be interpreted as in the same time zone in which the key is generated.
	 */
	LocalDateTime getStartTime(String key);

	/**
	 * Get the end time (exclusive) of the time period represented by the key.
	 * The key always marks the start time but in order to calculate the end time, time zone information is used to calculate the end time.
	 * @param key	the time period key
	 * @return	the end time (exclusive) of the time period
	 */
	ZonedDateTime getEndTime(String key);

	/**
	 * Generate the key representing the previous time period of a specified key
	 * @param key	the key for which the key for previous time period will be generated
	 * @return	the key identifying the previous time period
	 */
	String previousKey(String key);

	/**
	 * Generate the key representing the next time period of a specified key
	 * @param key	the key for which the key for next time period will be generated
	 * @return the key identifying the next time period
	 */
	String nextKey(String key);

	/**
	 * Generate the key representing the time period that the specified date time falls into
	 * @param aggregationPeriodCodeName		the code name of the aggregation period for which the key will be generated
	 * @param year			the year
	 * @param month	the month, valid values: [1, 12]
	 * @param dayOfMonth	the day in the month
	 * @param hour		the hour in the day, valid values: [0, 23]
	 * @param minute	the minute in the hour, valid values: [0, 59]
	 * @return	the time period key
	 */
	String generateKey(String aggregationPeriodCodeName, int year, int month, int dayOfMonth, int hour, int minute);

	/**
	 * Generate the key representing the time period that the specified date time falls into
	 * @param aggregationPeriod		the aggregation period for which the key will be generated
	 * @param year			the year
	 * @param month	the month, valid values: [1, 12]
	 * @param dayOfMonth	the day in the month
	 * @param hour		the hour in the day, valid values: [0, 23]
	 * @param minute	the minute in the hour, valid values: [0, 59]
	 * @return	the time period key
	 */
	default String generateKey(AggregationPeriod aggregationPeriod, int year, int month, int dayOfMonth, int hour, int minute){
		return generateKey(aggregationPeriod.getCodeName(), year, month, dayOfMonth, hour, minute);
	}
	
	/**
	 * Generate the key representing the time period that the specified date time falls into
	 * @param aggregationPeriodCodeName		the code name of the aggregation period for which the key will be generated
	 * @param dateTimeWithoutZone  the date time
	 * @return	the time period key
	 */
	String generateKey(String aggregationPeriodCodeName, LocalDateTime dateTimeWithoutZone);

	/**
	 * Generate the key representing the time period that the specified date time falls into
	 * @param aggregationPeriod		the aggregation period for which the key will be generated
	 * @param dateTimeWithoutZone  the date time
	 * @return	the time period key
	 */
	default String generateKey(AggregationPeriod aggregationPeriod, LocalDateTime dateTimeWithoutZone){
		return generateKey(aggregationPeriod.getCodeName(), dateTimeWithoutZone);
	}

	/**
	 * Generate the key representing the time period that the specified date time falls into
	 * @param aggregationPeriod		the aggregation period for which the key will be generated
	 * @param instant  the date time
	 * @return	the time period key
	 */
	default String generateKey(AggregationPeriod aggregationPeriod, Instant instant){
		return generateKey(aggregationPeriod.getCodeName(), LocalDateTime.ofInstant(instant, aggregationPeriod.getZone()));
	}

	/**
	 * Generate the upper level time period key representing the time period corresponding to the specified key
	 * @param key	the time period key
	 * @return	the upper level key, in the case there are more than one upper level keys, return the first/default one. 
	 * It can be null if this is already the highest level.
	 */
	String upperLevelKey(String key);
	
	/**
	 * In the case that there are more than one upper level definitions, use this method.
	 * @param key	the time period key
	 * @return		all the upper level keys, can be empty if this is already the highest level
	 */
	List<String> upperLevelKeys(String key);
	
	/**
	 * Generate the time period key representing the fist lower level time period
	 * @param key	the time period key
	 * @return	the lower level key
	 */
	String firstLowerLevelKey(String key);

	/**
	 * Retrieve the aggregation period information from the key
	 * @param key	the time period key
	 * @return		the aggregation period, or null if not found
	 */
	AggregationPeriod retrieveAggregationPeriod(String key);

	/**
	 * Separate the part representing AggregationPeriod from the key
	 * @param key	the time period key
	 * @return	An array that the first element is the code name of the AggregationPeriod or null if something went wrong, 
	 * 			and the second element is the remaining part of the key
	 */
	String[] separateAggregationPeriod(String key);

	/**
	 * Generate the number presentation of the key without the aggregation period part
	 * @param ap		the aggregation period
	 * @param year			the year
	 * @param month	the month, valid values: [1, 12]
	 * @param dayOfMonth	the day in the month
	 * @param hour		the hour in the day, valid values: [0, 23]
	 * @param minute	the minute in the hour, valid values: [0, 59]
	 * @return	the time period key represented as a number
	 */
	long generateKeyNumber(AggregationPeriod ap, int year, int month, int dayOfMonth, int hour, int minute);

	/**
	 * Generate the number presentation of the key without the aggregation period part
	 * @param ap		the aggregation period
	 * @param dateTimeWithoutZone	the data time
	 * @return	the time period key represented as a number
	 */
	long generateKeyNumber(AggregationPeriod ap, LocalDateTime dateTimeWithoutZone);

	/**
	 * Generate the number presentation of the key without the aggregation period part
	 * @param apCode		aggregation period code
	 * @param year			the year
	 * @param month	the month, valid values: [1, 12]
	 * @param dayOfMonth	the day in the month
	 * @param hour		the hour in the day, valid values: [0, 23]
	 * @param minute	the minute in the hour, valid values: [0, 59]
	 * @return	the time period key represented as a number
	 */
	long generateKeyNumber(String apCode, int year, int month, int dayOfMonth, int hour, int minute);

	/**
	 * Generate the number presentation of the key without the aggregation period part
	 * @param apCode		aggregation period code
	 * @param dateTimeWithoutZone	the data time
	 * @return	the time period key represented as a number
	 */
	long generateKeyNumber(String apCode, LocalDateTime dateTimeWithoutZone);
	
	/**
	 * Get the length of the key number
	 * @param ap	aggregation period
	 * @return	length of the key number
	 */
	int getKeyNumberLength(AggregationPeriod ap);

	/**
	 * Get the length of the key number
	 * @param apCode	code name of the aggregation period
	 * @return	length of the key number
	 */
	int getKeyNumberLength(String apCode);

	/**
	 * Generate the string form of the key
	 * @param ap		aggregation period
	 * @param keyNumber	number part of the key
	 * @return	the full string form of the key
	 */
	String generateKey(AggregationPeriod ap, long keyNumber);

	/**
	 * Generate the string form of the key
	 * @param apCode	code name of the aggregation period
	 * @param keyNumber	number part of the key
	 * @return	the full string form of the key
	 */
	String generateKey(String apCode, long keyNumber);


}
