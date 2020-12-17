package com.gentics.mesh.graphdb.cluster;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.etc.config.GraphStorageOptions;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.metric.MetricsService;

import io.micrometer.core.instrument.Counter;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import static com.gentics.mesh.metric.SimpleMetric.TX_INTERRUPT_COUNT;

/**
 * Task which terminates stalled or long running commit operations.
 */
@Singleton
public class TxCleanupTask implements Handler<Long> {

	private static final Logger log = LoggerFactory.getLogger(TxCleanupTask.class);

	private static final Map<Thread, Long> registeredThreads = new ConcurrentHashMap<>();

	private final Counter interruptCounter;

	private GraphStorageOptions storageOptions;

	@Inject
	public TxCleanupTask(MeshOptions options, MetricsService service) {
		this.storageOptions = options.getStorageOptions();
		this.interruptCounter = service.counter(TX_INTERRUPT_COUNT);
	}

	@Override
	public void handle(Long event) {
		checkTransactions();
	}

	/**
	 * Check whether there are any transactions which exceed the set time limit.
	 */
	public void checkTransactions() {
		if (log.isDebugEnabled()) {
			log.debug("Checking {} transaction threads", registeredThreads.size());
		}
		List<Thread> toInterrupt = registeredThreads.entrySet()
			.stream().filter(entry -> {
				long now = System.currentTimeMillis();
				long dur = now - entry.getValue();
				long limit = storageOptions.getTxCommitTimeout();
				boolean exceedsLimit = dur > limit;
				if (exceedsLimit) {
					log.warn("Thread {} exceeds time limit of {} with duration {}.", entry.getKey(), limit, dur);
				}
				return exceedsLimit;
			}).map(Map.Entry::getKey)
			.collect(Collectors.toList());

		interrupt(toInterrupt);
	}

	private void interrupt(List<Thread> toInterrupt) {
		if (log.isDebugEnabled()) {
			log.debug("Interrupting {} threads", toInterrupt.size());
		}
		for (Thread thread : toInterrupt) {
			interruptCounter.increment();
			log.info("Interrupting transaction thread {}", thread.getName());
			thread.interrupt();
		}
	}

	/**
	 * Register the thread that runs a tx.
	 * 
	 * @param thread
	 */
	public static void register(Thread thread) {
		registeredThreads.put(thread, System.currentTimeMillis());
	}

	/**
	 * Unregister the thread that runs tx.
	 * 
	 * @param thread
	 */
	public static void unregister(Thread thread) {
		registeredThreads.remove(thread);
	}

	/**
	 * Interrupt all active/registered transactions.
	 */
	public void interruptActive() {
		List<Thread> toInterrupt = registeredThreads.entrySet()
			.stream()
			.map(Map.Entry::getKey)
			.collect(Collectors.toList());

		interrupt(toInterrupt);
	}
}
