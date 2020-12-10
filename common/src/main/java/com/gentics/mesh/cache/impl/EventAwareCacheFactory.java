package com.gentics.mesh.cache.impl;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cache.EventAwareCache;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.metric.MetricsService;

import io.vertx.core.Vertx;

/**
 * Factory for {@link EventAwareCache} instances.
 */
@Singleton
public class EventAwareCacheFactory {

	private final Vertx vertx;
	private final MeshOptions meshOptions;
	private final MetricsService metricsService;

	@Inject
	public EventAwareCacheFactory(Vertx vertx, MeshOptions meshOptions, MetricsService metricsService) {
		this.vertx = vertx;
		this.meshOptions = meshOptions;
		this.metricsService = metricsService;
	}

	public <K, V> EventAwareCacheImpl.Builder<K, V> builder() {
		return new EventAwareCacheImpl.Builder<K, V>()
			.vertx(vertx)
			.meshOptions(meshOptions)
			.setMetricsService(metricsService);
	}
}
