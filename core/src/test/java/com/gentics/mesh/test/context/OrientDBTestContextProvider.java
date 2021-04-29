package com.gentics.mesh.test.context;

import com.gentics.mesh.MeshOptionsProvider;
import com.gentics.mesh.etc.config.MeshOptions;

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
