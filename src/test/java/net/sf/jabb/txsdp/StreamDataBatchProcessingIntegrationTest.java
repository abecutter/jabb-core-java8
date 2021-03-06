/**
 * 
 */
package net.sf.jabb.txsdp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.jms.BytesMessage;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;

import net.sf.jabb.azure.AzureEventHubUtility;
import net.sf.jabb.azure.EventHubAnnotations;
import net.sf.jabb.dstream.StreamDataSupplierWithId;
import net.sf.jabb.dstream.StreamDataSupplierWithIdAndRange;
import net.sf.jabb.dstream.WrappedJmsConnection;
import net.sf.jabb.dstream.eventhub.EventHubQpidStreamDataSupplier;
import net.sf.jabb.seqtx.azure.AzureSequentialTransactionsCoordinator;
import net.sf.jabb.seqtx.ex.TransactionStorageInfrastructureException;
import net.sf.jabb.txsdp.TransactionalStreamDataBatchProcessing.State;
import net.sf.jabb.txsdp.TransactionalStreamDataBatchProcessing.Status;
import net.sf.jabb.txsdp.DefaultTransactionalStreamDataBatchProcessing.Options;
import net.sf.jabb.util.bean.TripleValueBean;
import net.sf.jabb.util.col.PutIfAbsentMap;
import net.sf.jabb.util.jms.JmsUtility;
import net.sf.jabb.util.parallel.BackoffStrategies;
import net.sf.jabb.util.parallel.WaitStrategies;
import net.sf.jabb.util.stat.ConcurrentLongStatistics;

import org.apache.qpid.amqp_1_0.jms.impl.MessageImpl;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.microsoft.azure.storage.CloudStorageAccount;

/**
 * @author James Hu
 *
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class StreamDataBatchProcessingIntegrationTest {
	static private final Logger logger = LoggerFactory.getLogger(StreamDataBatchProcessingIntegrationTest.class);
	
	static final int NUM_PROCESSORS = 6;
	
	static protected AzureSequentialTransactionsCoordinator createCoordinator()  throws InvalidKeyException, URISyntaxException, TransactionStorageInfrastructureException{
		String connectionString = System.getenv("SYSTEM_DEFAULT_AZURE_STORAGE_CONNECTION");
		CloudStorageAccount storageAccount = CloudStorageAccount.parse(connectionString);
		AzureSequentialTransactionsCoordinator tracker = new AzureSequentialTransactionsCoordinator(storageAccount, "TestTable");
		tracker.clearAll();
		return tracker;
	}

	@Test
	public void test1OnlyOnePartition() throws Exception{
		doTest(1);
	}

	@Test
	public void test2AllPartitions() throws Exception {
		doTest(null);
	}
	
	@Test
	public void test3AllPartitionsStickySucceeded() throws Exception {
		doTest(null, Options.STICKY_WHEN_OPEN_RANGE_SUCCEEDED);
	}
	
	@Test
	public void test3AllPartitionsStickySucceededOrNoData() throws Exception {
		doTest(null, Options.STICKY_WHEN_OPEN_RANGE_SUCCEEDED_OR_NO_DATA);
	}
	
	protected void doTest(Integer numPartitions) throws Exception {
		doTest(numPartitions, Options.STICKY_NEVER);
	}
	
	protected void doTest(Integer numPartitions, int stickyMode) throws Exception {
		List<StreamDataSupplierWithId<TripleValueBean<String, Long, String>>> suppliersWithId = AzureEventHubUtility.createStreamDataSuppliers(
				System.getenv("SYSTEM_DEFAULT_AZURE_EVENT_HUB_HOST"),
				System.getenv("SYSTEM_DEFAULT_AZURE_EVENT_HUB_RECEIVE_USER_NAME"),
				System.getenv("SYSTEM_DEFAULT_AZURE_EVENT_HUB_RECEIVE_USER_PASSWORD"),
				System.getenv("SYSTEM_DEFAULT_AZURE_EVENT_HUB_NAME"),
				AzureEventHubUtility.DEFAULT_CONSUMER_GROUP,
				msg -> {
					TripleValueBean<String, Long, String> bean = new TripleValueBean<>();
					try {
						String queue = ((org.apache.qpid.amqp_1_0.jms.Destination)msg.getJMSDestination()).getAddress();
						Long sequence = AzureEventHubUtility.getEventHubAnnotations(msg).getSequenceNumber();
						String json = msg.getStringProperty(MessageImpl.JMS_AMQP_MESSAGE_ANNOTATIONS);
						bean.setValue1(queue);
						bean.setValue2(sequence);
						bean.setValue3(json);
					} catch (Exception e) {
						e.printStackTrace();
					}
					return bean;
				});
		assertTrue(suppliersWithId.size() >= 4);
		
		Instant rangeFrom = Instant.now().minus(Duration.ofMinutes(10));
		Instant rangeTo = Instant.now().plus(Duration.ofMinutes(2));
		List<StreamDataSupplierWithIdAndRange<TripleValueBean<String, Long, String>, ?>> suppliersWithIdAndRange = suppliersWithId.stream()
				.map(s->{
					try {
						s.getSupplier().start();
						return s.withRange(rangeFrom, rangeTo);
					} catch (Exception e) {
						return null;
					}
				})
				.filter(s -> s != null)
				.collect(Collectors.toList());
		assertEquals(suppliersWithId.size(), suppliersWithIdAndRange.size());
		
		if (numPartitions != null){
			while(suppliersWithIdAndRange.size() > numPartitions){
				suppliersWithIdAndRange.remove(0);
			}
		}
		
		for (StreamDataSupplierWithIdAndRange<TripleValueBean<String, Long, String>, ?> supplierWithIdAndRange: suppliersWithIdAndRange){
			logger.info("Range to be processed in {}: ({} - {}]", supplierWithIdAndRange.getId(), supplierWithIdAndRange.getFrom(), supplierWithIdAndRange.getTo());
			assertEquals(rangeFrom, supplierWithIdAndRange.getFrom());
			assertEquals(rangeTo, supplierWithIdAndRange.getTo());
		}

		
		Options options = new Options()
			.withInitialTransactionTimeoutDuration(Duration.ofMinutes(1))
			.withMaxInProgressTransactions(10)
			.withMaxRetringTransactions(10)
			.withTransactionAcquisitionDelay(Duration.ofSeconds(10))
			.withWaitStrategy(WaitStrategies.threadSleepStrategy())
			.withStickyMode(stickyMode);
		
		Map<String, Set<Long>> logMap = new PutIfAbsentMap<String, Set<Long>>(new HashMap<String, Set<Long>>(), k->Collections.newSetFromMap(new ConcurrentHashMap<Long, Boolean>()));
		
		TransactionalStreamDataBatchProcessing<TripleValueBean<String, Long, String>> processing = new DefaultTransactionalStreamDataBatchProcessing<>("TestJob", options, createCoordinator(), 
			(context, data) -> {
				if (data.size() > 0){
					String first = data.get(0).getValue3();
					String last = data.get(data.size() - 1).getValue3();
					logger.info("[{} {}] Processing {} items: {} - {}", context.getTransactionSeriesId(), context.getProcessorId(),
							data.size(), new EventHubAnnotations(first), new EventHubAnnotations(last));
					
					for (TripleValueBean<String, Long, String> bean: data){
						String queue = bean.getValue1();
						Long sequence = bean.getValue2();
						if (!logMap.get(queue).add(sequence)){
							if (numPartitions != null && numPartitions == 1){
								System.err.println("### (maybe) duplicated sequence number received in message: " + bean.getValue3());
							}
						}
					}
					
					try {
						long sleepTime = 100*data.size();
						if (sleepTime > 2*1000L){
							sleepTime = 2*1000L;
						}
						Thread.sleep(sleepTime);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
				}
				return true;
		}, 3000, Duration.ofSeconds(60), Duration.ofSeconds(25),
		suppliersWithIdAndRange);
		
		ExecutorService threadPool = Executors.newCachedThreadPool();
		for (int i = 0; i < NUM_PROCESSORS; i ++){
			Runnable runnable = processing.createProcessor(String.valueOf(i));
			threadPool.execute(runnable);
		}
		
		logger.info("Starting {} processors in their threads", NUM_PROCESSORS);
		processing.startAll();
		
		Queue eventHub = EventHubQpidStreamDataSupplier.createQueue(System.getenv("SYSTEM_DEFAULT_AZURE_EVENT_HUB_NAME"));
		WrappedJmsConnection sendConnection = EventHubQpidStreamDataSupplier.createConnectionForSending(
				System.getenv("SYSTEM_DEFAULT_AZURE_EVENT_HUB_HOST"),
				System.getenv("SYSTEM_DEFAULT_AZURE_EVENT_HUB_SEND_USER_NAME"),
				System.getenv("SYSTEM_DEFAULT_AZURE_EVENT_HUB_SEND_USER_PASSWORD"),
				eventHub, 
				BackoffStrategies.fibonacciBackoff(1000, 10, TimeUnit.SECONDS), 
				WaitStrategies.threadSleepStrategy(), 
				"test");
		sendConnection.establishConnection();
		
		for (int i = Integer.MAX_VALUE; i >= 0; i --){
			Session session = null;
			MessageProducer sender = null;
			try{
				while(sendConnection.getConnection() == null){
					logger.debug("Wait for send connection to be initially established");
					Thread.sleep(100);
				}
				session = sendConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
				sender = session.createProducer(eventHub);
				for (int id = 0; id <= 100; id ++){
					BytesMessage msg = session.createBytesMessage();
					byte[] bytes = ("{\"id\"=" + id + "}").getBytes(StandardCharsets.UTF_8);
					msg.writeBytes(bytes);
					sender.send(msg);
				}
			}catch(Exception e){
				e.printStackTrace();
			}finally{
				JmsUtility.closeSilently(sender, null, session);
			}
			Thread.sleep(Duration.ofSeconds(30).toMillis());
			try{
				Status status = processing.getStatus();
				logger.info("Status: {}", status);
				
				if (status.getProcessorStatus().values().stream().filter(s->s.getState() == State.FINISHED)
					.count() == NUM_PROCESSORS){
					logger.info("All finished");
					break;
				}
				
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		
		for (StreamDataSupplierWithIdAndRange<TripleValueBean<String, Long, String>, ?> supplierWithIdAndRange: suppliersWithIdAndRange){
			supplierWithIdAndRange.getSupplier().stop();
		}
		
		threadPool.shutdown();
		
		if (numPartitions != null && numPartitions == 1){
			for (Map.Entry<String, Set<Long>> entry: logMap.entrySet()){
				String queue = entry.getKey();
				Set<Long> seqs = entry.getValue();
				int size = seqs.size();
				ConcurrentLongStatistics stats = new ConcurrentLongStatistics();
				for (Long s: seqs){
					stats.evaluate(s);
				}
				System.out.println("Processed " + size + " messages in: " + queue);
				assertEquals(size, stats.getMax() - stats.getMin() + 1);
			}
		}
	}

}
