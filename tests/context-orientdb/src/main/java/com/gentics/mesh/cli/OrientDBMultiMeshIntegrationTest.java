package com.gentics.mesh.cli;

import org.junit.Ignore;

import com.gentics.mesh.etc.config.OrientDBMeshOptions;

@Ignore
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
