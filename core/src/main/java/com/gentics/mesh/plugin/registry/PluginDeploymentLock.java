package com.gentics.mesh.plugin.registry;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.etc.config.MeshOptions;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ILock;

import dagger.Lazy;

/**
 * The plugin deployment lock prevents concurrent initialization of plugins in cluster setups.
 */
@Singleton
public class PluginDeploymentLock implements AutoCloseable {

	public static final String GLOBAL_PLUGIN_LOCK_KEY = "MESH_PLUGIN_LOCK";

	private ILock clusterLock;

	private final MeshOptions options;

	private final boolean isClustered;

	private final Lazy<HazelcastInstance> hazelcast;

	@Inject
	public PluginDeploymentLock(MeshOptions options, Lazy<HazelcastInstance> hazelcast) {
		this.options = options;
		this.isClustered = options.getClusterOptions().isEnabled();
		this.hazelcast = hazelcast;
	}

	@Override
	public void close() {
		if (isClustered) {
			if (clusterLock != null && clusterLock.isLockedByCurrentThread()) {
				System.out.println("Unlock");
				clusterLock.unlock();
			}
		}
	}

	/**
	 * Locks the global plugin lock.
	 * 
	 * @return
	 */
	public PluginDeploymentLock lock() {
		// No need to lock in non-clustered setups.
		if (!isClustered) {
			return this;
		}

		// Lets use twice the plugin timeout to be on the safe side.
		long timeout = 2 * options.getPluginTimeout();
		try {
			if (clusterLock == null) {
				HazelcastInstance hz = hazelcast.get();
				if (hz != null) {
					this.clusterLock = hz.getLock(GLOBAL_PLUGIN_LOCK_KEY);
				}
				if (clusterLock != null) {
					boolean isTimeout = !clusterLock.tryLock(timeout, TimeUnit.SECONDS);
					if (isTimeout) {
						throw new RuntimeException("Got timeout while waiting for plugin lock.");
					}
				}
			}
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}

		return this;
	}

}
