package com.gentics.mesh.cli;

import com.gentics.mesh.etc.config.GraphDBMeshOptions;

public class OrientDBMeshIntegrationTest extends MeshMeshIntegrationTest<GraphDBMeshOptions>  {

	@Override
	public GraphDBMeshOptions getOptions() {
		return new GraphDBMeshOptions();
	}

	@Override
	void setupOptions(GraphDBMeshOptions options) {
		options.getStorageOptions().setDirectory(null);		
	}

}
