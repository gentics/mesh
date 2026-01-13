package com.gentics.mesh.core;

import com.gentics.mesh.etc.config.HibernateMeshOptions;
import com.gentics.mesh.test.MeshOptionsProvider;
import com.gentics.mesh.test.MeshTestContextProvider;

public class ExternalDatabaseTestContextProvider extends HibernateTestContextProvider implements MeshTestContextProvider {

	static {
		System.setProperty(MeshOptionsProvider.ENV_OPTIONS_PROVIDER_CLASS, ExternalDatabaseTestContextProvider.class.getCanonicalName());
	}

	@Override
	public void fillMeshOptions(HibernateMeshOptions meshOptions) {
		// Taken from the env variables of HibernateMeshOptions
	}

}
