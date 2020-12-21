package com.gentics.mesh.search.index.metric;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.metric.MetricsService;

/**
 * 
 */
@Singleton
public class SyncMetersFactory {

	private final MetricsService registry;

	private final Map<String, SyncMeters> meters = new ConcurrentHashMap<>();

	@Inject
	public SyncMetersFactory(MetricsService registry) {
		this.registry = registry;
	}

	/**
	 * Create a new ES sync meter for (insert, delete, update).
	 * 
	 * @param type
	 * @return
	 */
	public SyncMeters createSyncMetric(String type) {
		return meters.computeIfAbsent(type, k -> new SyncMeters(registry, k));
	}

	/**
	 * Reset all meters.
	 */
	public void reset() {
		meters.values().forEach(SyncMeters::reset);
	}
}
