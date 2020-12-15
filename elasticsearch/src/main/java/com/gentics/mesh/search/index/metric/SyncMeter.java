package com.gentics.mesh.search.index.metric;

import java.util.concurrent.atomic.AtomicLong;

import com.gentics.mesh.core.rest.search.TypeMetrics;
import com.gentics.mesh.metric.MetricsService;

/**
 * Metric meter which tracks sync operations.
 */
public class SyncMeter {

	private final AtomicLong synced;
	private final AtomicLong pending;

	public SyncMeter(MetricsService metricsService, String type, SyncMetric.Operation operation) {
		pending = metricsService.longGauge(new SyncMetric(type, operation, SyncMetric.Meter.PENDING));
		synced = metricsService.longGauge(new SyncMetric(type, operation, SyncMetric.Meter.SYNCED));
	}

	/**
	 * Add given amount to pending counter.
	 * 
	 * @param amount
	 */
	public void addPending(long amount) {
		pending.addAndGet(amount);
	}

	/**
	 * Track a synced operation which will increment synced and decrement pending.
	 */
	public void synced() {
		pending.decrementAndGet();
		synced.incrementAndGet();
	}

	/**
	 * Reset the pending and synced counters.
	 */
	public void reset() {
		synced.set(0);
		pending.set(0);
	}

	/**
	 * Return the synced counter value.
	 * 
	 * @return
	 */
	public long getSynced() {
		return synced.get();
	}

	/**
	 * Return the pending counter value.
	 * 
	 * @return
	 */
	public long getPending() {
		return pending.get();
	}

	/**
	 * Create a type metrics snapshot to be used for REST responses.
	 * 
	 * @return
	 */
	public TypeMetrics createSnapshot() {
		return new TypeMetrics()
			.setPending(pending.get())
			.setSynced(synced.get());
	}
}
