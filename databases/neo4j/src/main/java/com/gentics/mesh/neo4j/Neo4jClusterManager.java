package com.gentics.mesh.neo4j;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.rest.admin.cluster.ClusterStatusResponse;
import com.gentics.mesh.graphdb.cluster.ClusterManager;
import com.hazelcast.core.HazelcastInstance;

@Singleton
public class Neo4jClusterManager implements ClusterManager {

	@Inject
	public Neo4jClusterManager() {
	}

	@Override
	public void initConfigurationFiles() throws IOException {

	}

	@Override
	public void start() throws Exception {

	}

	@Override
	public HazelcastInstance getHazelcast() {
		return null;
	}

	@Override
	public ClusterStatusResponse getClusterStatus() {
		return null;
	}

	@Override
	public void stop() {

	}

	@Override
	public void registerEventHandlers() {

	}

	@Override
	public void stopHazelcast() {

	}

}
