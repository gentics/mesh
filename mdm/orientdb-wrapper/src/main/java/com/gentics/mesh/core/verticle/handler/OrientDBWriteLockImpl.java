package com.gentics.mesh.core.verticle.handler;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.db.cluster.ClusterManager;
import com.gentics.mesh.etc.config.GraphDBMeshOptions;
import com.gentics.mesh.metric.MetricsService;
import com.hazelcast.core.HazelcastInstance;

import dagger.Lazy;

/**
 * @see WriteLock
 */
@Singleton
public class OrientDBWriteLockImpl extends AbstractGenericWriteLock {

	private final GraphDBMeshOptions options;

	@Inject
	public OrientDBWriteLockImpl(GraphDBMeshOptions options, Lazy<HazelcastInstance> hazelcast, MetricsService metricsService, ClusterManager clusterManager) {
		super(options, hazelcast, metricsService, clusterManager);
		this.options = options;
	}

	public boolean isSyncWrites() {
		return options.getStorageOptions().isSynchronizeWrites();
	}

	@Override
	protected long getSyncWritesTimeoutMillis() {
		return options.getStorageOptions().getSynchronizeWritesTimeout();
	}
}
