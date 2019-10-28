package com.gentics.mesh.search.index.metric;

import java.util.concurrent.atomic.AtomicLong;

import com.gentics.mesh.metric.MetricsService;
import com.gentics.mesh.metric.SyncMetric;

import io.micrometer.core.instrument.DistributionSummary;

public class SyncMeter {
	private final DistributionSummary pendingHistogram;
	private final DistributionSummary syncedCounter;
	private final AtomicLong pendingGauge;

	public SyncMeter(MetricsService metricsService, String type, SyncMetric.Operation operation) {
		pendingHistogram = metricsService.meter(new SyncMetric(type, operation, SyncMetric.Meter.PENDING));
		syncedCounter = metricsService.meter(new SyncMetric(type, operation, SyncMetric.Meter.TOTAL));
		pendingGauge = metricsService.longGauge(new SyncMetric(type, operation, SyncMetric.Meter.PENDINGGAUGE));
	}

	public void addPending(int amount) {
		pendingHistogram.record(amount);
		pendingGauge.addAndGet(amount);
	}

	public void synced() {
		syncedCounter.record(1);
		pendingGauge.decrementAndGet();
	}

	public void reset() {
		pendingGauge.set(0);
	}

	public long getTotalSynced() {
		return syncedCounter.count();
	}

	public long getCurrentPending() {
		return pendingGauge.get();
	}
}
