package com.gentics.mesh.cache.impl;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cache.EventAwareCache;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.event.EventBusStore;
import com.gentics.mesh.metric.MetricsService;

/**
 * Factory for {@link EventAwareCache} instances.
 */
@Singleton
public class EventAwareCacheFactory {
	private final EventBusStore eventBusStore;
	private final MeshOptions meshOptions;
	private final MetricsService metricsService;

	@Inject
	public EventAwareCacheFactory(EventBusStore eventBusStore, MeshOptions meshOptions, MetricsService metricsService) {
		this.eventBusStore = eventBusStore;
		this.meshOptions = meshOptions;
		this.metricsService = metricsService;
	}

	/**
	 * Create a builder for caches.
	 * 
	 * @param <K>
	 * @param <V>
	 * @return
	 */
	public <K, V> EventAwareCacheImpl.Builder<K, V> builder() {
		return new EventAwareCacheImpl.Builder<K, V>()
			.eventBusStore(eventBusStore)
			.meshOptions(meshOptions)
			.setMetricsService(metricsService);
	}
}
