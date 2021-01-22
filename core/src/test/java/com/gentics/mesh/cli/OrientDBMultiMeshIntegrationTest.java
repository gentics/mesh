package com.gentics.mesh.cli;

import com.gentics.mesh.etc.config.OrientDBMeshOptions;

public class OrientDBMultiMeshIntegrationTest extends MultiMeshIntegrationTest<OrientDBMeshOptions> {

	@Override
	public OrientDBMeshOptions getOptions() {
		return new OrientDBMeshOptions();
	}

	@Override
	protected void setupOptions(OrientDBMeshOptions option, int i) {
		option.getStorageOptions().setDirectory("data/m" + i);		
	}

}
