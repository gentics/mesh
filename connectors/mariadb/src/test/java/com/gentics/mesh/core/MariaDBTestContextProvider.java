package com.gentics.mesh.core;

import com.gentics.mesh.etc.config.HibernateMeshOptions;
import com.gentics.mesh.test.docker.DatabaseContainer;
import com.gentics.mesh.test.docker.MariaDBDatabaseContainer;
import com.gentics.mesh.test.MeshOptionsProvider;
import com.gentics.mesh.test.MeshTestContextProvider;

public class MariaDBTestContextProvider extends PreparingDatabaseTestContextProvider<MariaDBDatabaseContainer> implements MeshTestContextProvider, MariaDBTestContextProviderBase {

	static {
		System.setProperty(MeshOptionsProvider.ENV_OPTIONS_PROVIDER_CLASS, MariaDBTestContextProvider.class.getCanonicalName());
	}

	public MariaDBTestContextProvider() {
		super(new MariaDBDatabaseContainer());
	}

	@Override
	public void fillMeshOptions(HibernateMeshOptions options) {
		super.fillMeshOptions(options);
		options.getStorageOptions().setDatabaseName(current);
		options.getStorageOptions().setConnectionUsername(DatabaseContainer.DEFAULT_USERNAME);
		options.getStorageOptions().setConnectionPassword(DatabaseContainer.DEFAULT_PASSWORD);
	}
}
