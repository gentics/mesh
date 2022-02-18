package com.gentics.mesh.core.verticle.handler;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.db.cluster.ClusterManager;
import com.gentics.mesh.etc.config.OrientDBMeshOptions;
import com.gentics.mesh.metric.MetricsService;
import com.hazelcast.core.HazelcastInstance;

import dagger.Lazy;

/**
 * @see WriteLock
 */
@Singleton
public class OrientDBWriteLockImpl extends AbstractGenericWriteLock {

	private final OrientDBMeshOptions options;

	@Inject
	public OrientDBWriteLockImpl(OrientDBMeshOptions options, Lazy<HazelcastInstance> hazelcast, MetricsService metricsService, ClusterManager clusterManager) {
		super(options, hazelcast, metricsService, clusterManager);
		this.options = options;
	}

	protected boolean isSyncWrites() {
		return options.getStorageOptions().isSynchronizeWrites();
	}

	@Override
	protected long getSyncWritesTimeoutMillis() {
		return options.getStorageOptions().getSynchronizeWritesTimeout();
	}
}
