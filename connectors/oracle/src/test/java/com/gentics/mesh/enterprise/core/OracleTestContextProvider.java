package com.gentics.mesh.enterprise.core;

import com.gentics.mesh.enterprise.etc.config.EnterpriseMeshOptions;
import com.gentics.mesh.enterprise.test.docker.DatabaseContainer;
import com.gentics.mesh.enterprise.test.docker.OracleDatabaseContainer;
import com.gentics.mesh.test.MeshOptionsProvider;
import com.gentics.mesh.test.MeshTestContextProvider;

public class OracleTestContextProvider extends PreparingDatabaseTestContextProvider<OracleDatabaseContainer> implements MeshTestContextProvider {

	static {
		System.setProperty(MeshOptionsProvider.ENV_OPTIONS_PROVIDER_CLASS, OracleTestContextProvider.class.getCanonicalName());
	}

	public OracleTestContextProvider() {
		super(new OracleDatabaseContainer());
	}

	@Override
	public void fillMeshOptions(EnterpriseMeshOptions options) {
		super.fillMeshOptions(options);
		options.getStorageOptions().setConnectionUrl("jdbc:oracle:thin:@localhost:" + getDatabaseContainer().getMappedPort() + "/");
		options.getStorageOptions().setDatabaseName(DatabaseContainer.DEFAULT_DATABASE);
		options.getStorageOptions().setConnectionUsername(current);
		options.getStorageOptions().setConnectionPassword(DatabaseContainer.DEFAULT_PASSWORD);
	}
}
