package com.gentics.mesh.cli;

import com.gentics.mesh.etc.config.OrientDBMeshOptions;

public class OrientDBMeshIntegrationTest extends MeshMeshIntegrationTest<OrientDBMeshOptions>  {

	@Override
	public OrientDBMeshOptions getOptions() {
		return new OrientDBMeshOptions();
	}

	@Override
	void setupOptions(OrientDBMeshOptions options) {
		options.getStorageOptions().setDirectory(null);		
	}

}
