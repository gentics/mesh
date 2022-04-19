package com.gentics.mesh.test.orientdb;


import com.gentics.mesh.OptionsLoader;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.OrientDBMeshOptions;
import com.gentics.mesh.test.MeshOptionsProvider;

public class OrientDBMeshOptionsProvider implements MeshOptionsProvider {

	private final OrientDBMeshOptions meshOptions;
	
	public OrientDBMeshOptionsProvider() {
		meshOptions = OptionsLoader.generateDefaultConfig(OrientDBMeshOptions.class, null);
	}
	
	@Override
	public MeshOptions getOptions() {
		return meshOptions;
	}
}
