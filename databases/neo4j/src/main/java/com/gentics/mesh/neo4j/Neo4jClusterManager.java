package com.gentics.mesh.neo4j;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.rest.admin.cluster.ClusterStatusResponse;
import com.gentics.mesh.graphdb.cluster.ClusterManager;

@Singleton
public class 
Neo4jClusterManager implements ClusterManager {

	@Inject
	public Neo4jClusterManager() {
	}

	@Override
	public void initConfigurationFiles() throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void start() throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Object getHazelcast() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ClusterStatusResponse getClusterStatus() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void registerEventHandlers() {
		// TODO Auto-generated method stub
		
	}
	
}
