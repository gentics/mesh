package com.gentics.mesh.enterprise.core;

import com.gentics.mesh.core.PreparingDatabaseTestContextProvider;
import com.gentics.mesh.enterprise.test.docker.OracleDatabaseContainer;
import com.gentics.mesh.etc.config.HibernateMeshOptions;
import com.gentics.mesh.test.MeshOptionsProvider;
import com.gentics.mesh.test.MeshTestContextProvider;
import com.gentics.mesh.test.docker.DatabaseContainer;

public class OracleTestContextProvider extends PreparingDatabaseTestContextProvider<OracleDatabaseContainer> implements MeshTestContextProvider {

	static {
		System.setProperty(MeshOptionsProvider.ENV_OPTIONS_PROVIDER_CLASS, OracleTestContextProvider.class.getCanonicalName());
	}

	public OracleTestContextProvider() {
		super(new OracleDatabaseContainer());
	}

	@Override
	public void fillMeshOptions(HibernateMeshOptions options) {
		super.fillMeshOptions(options);
		options.getStorageOptions().setDatabaseName(DatabaseContainer.DEFAULT_DATABASE);
		options.getStorageOptions().setConnectionUsername(current);
		options.getStorageOptions().setConnectionPassword(DatabaseContainer.DEFAULT_PASSWORD);
	}
}
