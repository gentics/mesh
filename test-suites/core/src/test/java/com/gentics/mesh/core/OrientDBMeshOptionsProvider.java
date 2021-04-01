package com.gentics.mesh.core;

import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.OrientDBMeshOptions;
import com.gentics.mesh.test.MeshOptionsProvider;

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
