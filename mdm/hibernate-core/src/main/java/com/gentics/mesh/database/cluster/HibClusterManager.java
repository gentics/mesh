package com.gentics.mesh.database.cluster;

import java.io.IOException;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.db.cluster.ClusterManager;
import com.gentics.mesh.core.rest.admin.cluster.ClusterStatusResponse;
import com.gentics.mesh.etc.config.ClusterOptions;
import com.gentics.mesh.etc.config.HibernateMeshOptions;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

import dagger.Lazy;
import io.reactivex.Completable;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;

/**
 * Hibernate DB cluster manager. Either creates its own Hazelcast instance, or reuses an existing 2nd level cache, built atop Hazelcast.
 * 
 * @author plyhun
 *
 */
@Singleton
public class HibClusterManager implements ClusterManager {

	private final ClusterOptions clusterOptions;
	private final Lazy<Database> db;
	private HazelcastClusterManager clusterManager;

	@Override
	public HazelcastInstance getHazelcast() {
		if (clusterManager == null) {
			return Hazelcast.getAllHazelcastInstances().stream().findFirst().orElse(null);
		}
		return clusterManager.getHazelcastInstance();
	}

	@Override
	public io.vertx.core.spi.cluster.ClusterManager getVertxClusterManager() {
		if (clusterManager == null) {
			Optional<HazelcastInstance> hazelcastInstance = Hazelcast.getAllHazelcastInstances().stream().findFirst();
			clusterManager = hazelcastInstance
					// when using second level cache, we already have an hazelcast instance
					.map(instance -> new HazelcastClusterManager(instance))
					.orElseGet(HazelcastClusterManager::new);
		}
		return clusterManager;
	}

	@Inject
	public HibClusterManager(HibernateMeshOptions options, Lazy<Database> db) {
		this.clusterOptions = options.getClusterOptions();
		this.db = db;
	}

	@Override
	public void initConfigurationFiles() throws IOException {

	}

	@Override
	public ClusterStatusResponse getClusterStatus() {
		// database clustering is not managed directly by mesh hibernate, hence we return an empty response
		return new ClusterStatusResponse();
	}

	@Override
	public Completable waitUntilWriteQuorumReached() {
		return Completable.complete();
	}

	@Override
	public boolean isClusterTopologyLocked() {
		return false;
	}

	@Override
	public boolean isLocalNodeOnline() {
		return true;
	}

	@Override
	public boolean isWriteQuorumReached() {
		return true;
	}

	@Override
	public Completable waitUntilLocalNodeOnline() {
		return Completable.complete();
	}
}
