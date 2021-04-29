package com.gentics.mesh.test.context;

import com.gentics.mesh.MeshOptionsProvider;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.OrientDBMeshOptions;

public class OrientDBMeshOptionsProvider implements MeshOptionsProvider {

	private final OrientDBMeshOptions meshOptions;

	public OrientDBMeshOptionsProvider() {
		meshOptions = new OrientDBMeshOptions();
	}

	@Override
	public MeshOptions getOptions() {
		return meshOptions;
	}
}
