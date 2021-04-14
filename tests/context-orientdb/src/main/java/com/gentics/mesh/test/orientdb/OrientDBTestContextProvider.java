package com.gentics.mesh.test.orientdb;


import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.test.MeshInstanceProvider;
import com.gentics.mesh.test.MeshOptionsProvider;
import com.gentics.mesh.test.MeshTestContextProvider;

public class OrientDBTestContextProvider extends OrientDBMeshOptionsProvider implements MeshTestContextProvider {
	
	private final OrientDBMeshInstanceProvider instanceProvider;
	
	static {
		System.setProperty(MeshOptionsProvider.ENV_OPTIONS_PROVIDER_CLASS, OrientDBTestContextProvider.class.getCanonicalName());
	}
	
	public OrientDBTestContextProvider() {
		this.instanceProvider = new OrientDBMeshInstanceProvider(getOptions());
	}

	@Override
	public MeshInstanceProvider<? extends MeshOptions> getInstanceProvider() {
		return instanceProvider;
	}
}
