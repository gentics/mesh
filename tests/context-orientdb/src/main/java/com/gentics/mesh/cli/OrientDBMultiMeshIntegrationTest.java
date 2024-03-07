package com.gentics.mesh.cli;

import org.junit.Ignore;

import com.gentics.mesh.etc.config.GraphDBMeshOptions;

@Ignore
public class OrientDBMultiMeshIntegrationTest extends MultiMeshIntegrationTest<GraphDBMeshOptions> {

	@Override
	public GraphDBMeshOptions getOptions() {
		return new GraphDBMeshOptions();
	}

	@Override
	protected void setupOptions(GraphDBMeshOptions option, int i) {
		option.getStorageOptions().setDirectory("data/m" + i);		
	}

}
