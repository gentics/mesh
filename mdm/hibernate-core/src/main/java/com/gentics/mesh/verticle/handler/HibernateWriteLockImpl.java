package com.gentics.mesh.verticle.handler;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.db.cluster.ClusterManager;
import com.gentics.mesh.core.verticle.handler.AbstractGenericWriteLock;
import com.gentics.mesh.core.verticle.handler.WriteLock;
import com.gentics.mesh.etc.config.HibernateMeshOptions;
import com.gentics.mesh.metric.MetricsService;
import com.hazelcast.core.HazelcastInstance;

import dagger.Lazy;

/**
 * Simple on-demand {@link WriteLock}.
 * 
 * @author plyhun
 *
 */
@Singleton
public class HibernateWriteLockImpl extends AbstractGenericWriteLock {

	private final HibernateMeshOptions options;

	@Inject
	public HibernateWriteLockImpl(HibernateMeshOptions options, Lazy<HazelcastInstance> hazelcast, MetricsService metricsService, ClusterManager clusterManager) {
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

