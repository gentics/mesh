package com.gentics.mesh.search.index.metric;

import java.util.concurrent.atomic.AtomicLong;

import com.gentics.mesh.core.rest.search.TypeMetrics;
import com.gentics.mesh.metric.MetricsService;
import com.gentics.mesh.metric.SyncMetric;

public class SyncMeter {
	private final AtomicLong synced;
	private final AtomicLong pending;

	public SyncMeter(MetricsService metricsService, String type, SyncMetric.Operation operation) {
		pending = metricsService.longGauge(new SyncMetric(type, operation, SyncMetric.Meter.PENDING));
		synced = metricsService.longGauge(new SyncMetric(type, operation, SyncMetric.Meter.SYNCED));
	}

	public void addPending(long amount) {
		pending.addAndGet(amount);
	}

	public void synced() {
		pending.decrementAndGet();
		synced.incrementAndGet();
	}

	public void reset() {
		synced.set(0);
		pending.set(0);
	}

	public long getSynced() {
		return synced.get();
	}

	public long getPending() {
		return pending.get();
	}

	public TypeMetrics createSnapshot() {
		return new TypeMetrics()
			.setPending(pending.get())
			.setSynced(synced.get());
	}
}
