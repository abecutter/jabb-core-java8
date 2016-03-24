/**
 * 
 */
package net.sf.jabb.txsdp;

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;

/**
 * Context for the transactional batch processing of stream data.
 * Implementations of this interface are normally not thread safe.
 * @author James Hu
 *
 */
public interface ProcessingContext {
	
	/**
	 * Renew the timeout of this batch processing.
	 * Value returned by {@link #getTransactionTimeout()} will reflect the new timeout if this method succeeded.
	 * @param newTimeout	the new timeout
	 * @return	true if successfully renewed, false otherwise
	 */
	boolean renewTransactionTimeout(Instant newTimeout);
	
	/**
	 * Renew the timeout of this batch processing.
	 * Value returned by {@link #getTransactionTimeout()} will reflect the new timeout if this method succeeded.
	 * @param newTimeoutDuration	the new duration after which the timeout will happen
	 * @return	true if successfully renewed, false otherwise
	 */
	default boolean renewTransactionTimeout(Duration newTimeoutDuration){
		return renewTransactionTimeout(Instant.now().plus(newTimeoutDuration));
	}
	
	/**
	 * Get the time out of this transactional batch processing.
	 * If {@link #renewTransactionTimeout(Instant)} or {@link #renewTransactionTimeout(Duration)} succeeded, 
	 * the value returned by this method will reflect the updated timeout.
	 * @return the time after which the transaction will time out
	 */
	Instant getTransactionTimeout();
	
	/**
	 * Get the ID of the transaction series. This information is useful for logging.
	 * @return	the transaction series ID
	 */
	String getTransactionSeriesId();
	
	/**
	 * Get the processor ID. This information is useful for logging.
	 * @return	processor ID
	 */
	String getProcessorId();
	
	/**
	 * Get the transaction ID. This information is useful for logging.
	 * @return	transaction ID
	 */
	String getTransactionId();

	/**
	 * Get the start position of the transaction.
	 * @return	the start position of the transaction
	 */
	String getTransactionStartPosition();
	
	/**
	 * Get the end position of the transaction
	 * @return	the end position of the transaction
	 */
	String getTransactionEndPosition();
	
	/**
	 * Get the detail field of the transaction
	 * If {@link #updateTransactionDetail(Serializable)} succeeded, 
	 * the value returned by this method will reflect the updated detail.
	 * @return	detail field of the transaction
	 */
	Serializable getTransactionDetail();
	
	/**
	 * Get the number of attempts for the transaction
	 * @return 1 if the transaction has been attempted once, 
	 * 			2 if the transaction has failed once and later been retried once, 
	 * 			3 if the transaction has been tried and retried three times, etc.
	 */
	int getTransactionAttempts();

	/**
	 * Update the transaction with a new detail field.
	 * Value returned by {@link #getTransactionDetail()} will reflect the new timeout if this method succeeded.
	 * @param newDetail	the new detail
	 * @return	true if succeeded, false otherwise
	 */
	boolean updateTransactionDetail(Serializable newDetail);

	/**
	 * Put something into the context. It is not guaranteed to be thread safe.
	 * @param key		the key that can be used later for retrieval
	 * @param value		the value object
	 * @return		previous value associated with the key if exist, or null
	 */
	Object put(String key, Object value);

	/**
	 * Get previously put value object from the context. It is not guaranteed to be thread safe.
	 * @param key		the key previously used to put the value object
	 * @return			the value object associated with the key
	 */
	Object get(String key);
	
	/**
	 * Remove from the context if it exist.
	 * @param key   the key previously used to put the value object
	 * @return	the value previously associated with the key, or null if not found
	 */
	Object remove(String key);

	/**
	 * Get the transaction finisher that can be used to finish or abort the transaction.
	 * @return	the transaction finisher that is detached from the context.
	 */
	TransactionFinisher getTransactionFinisher();
	
	/**
	 * An object that can be detached from the context for handling the finishing and aborting of the transaction
	 * @author James Hu
	 *
	 */
	public static interface TransactionFinisher{
		/**
		 * Finish the transaction
		 * @return	true if successfully finished, false otherwise
		 */
		boolean finishTransaction();
		
		/**
		 * Abort the transaction
		 * @return	true if successfully aborted, false otherwise
		 */
		boolean abortTransaction();

		/**
		 * Renew timeout for the transaction
		 * @param newTimeout	the new timeout
		 * @return	true if successfully renewed, false otherwise
		 */
		boolean renewTransactionTimeout(Instant newTimeout);

		/**
		 * Update detail for the transaction
		 * @param newDetail	the new detail
		 * @return	true if successfully updated, false otherwise
		 */
		boolean updateTransactionDetail(Serializable newDetail);
	}


}
