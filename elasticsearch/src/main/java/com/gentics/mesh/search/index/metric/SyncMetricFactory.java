package com.gentics.mesh.search.index.metric;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.micrometer.core.instrument.MeterRegistry;

@Singleton
public class SyncMetricFactory {
	private final MeterRegistry registry;

	@Inject
	public SyncMetricFactory(MeterRegistry registry) {
		this.registry = registry;
	}

	public SyncMetric createSyncMetric(String type) {
		return new SyncMetric(type);
	}
}
