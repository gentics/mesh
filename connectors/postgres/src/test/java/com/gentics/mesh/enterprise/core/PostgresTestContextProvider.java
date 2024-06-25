package com.gentics.mesh.enterprise.core;

import com.gentics.mesh.enterprise.etc.config.EnterpriseMeshOptions;
import com.gentics.mesh.enterprise.test.docker.DatabaseContainer;
import com.gentics.mesh.enterprise.test.docker.PostgresDatabaseContainer;
import com.gentics.mesh.test.MeshOptionsProvider;
import com.gentics.mesh.test.MeshTestContextProvider;

public class PostgresTestContextProvider extends PreparingDatabaseTestContextProvider<PostgresDatabaseContainer> implements MeshTestContextProvider {

	static {
		System.setProperty(MeshOptionsProvider.ENV_OPTIONS_PROVIDER_CLASS, PostgresTestContextProvider.class.getCanonicalName());
	}

	public PostgresTestContextProvider() {
		super(new PostgresDatabaseContainer());
	}

	@Override
	public void fillMeshOptions(EnterpriseMeshOptions options) {
		super.fillMeshOptions(options);
		options.getStorageOptions().setConnectionUrl("jdbc:postgresql://localhost:" + getDatabaseContainer().getMappedPort() + "/");
		options.getStorageOptions().setConnectionUrlExtraParams("?autosave=always&currentSchema=" + current);
		options.getStorageOptions().setDatabaseName(DatabaseContainer.DEFAULT_DATABASE);
		options.getStorageOptions().setConnectionUsername(DatabaseContainer.DEFAULT_USERNAME);
		options.getStorageOptions().setConnectionPassword(DatabaseContainer.DEFAULT_PASSWORD);
	}
}
