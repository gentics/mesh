package com.gentics.mesh.enterprise.core;

import com.gentics.mesh.enterprise.etc.config.EnterpriseMeshOptions;
import com.gentics.mesh.enterprise.test.docker.DatabaseContainer;
import com.gentics.mesh.enterprise.test.docker.SQLServerDatabaseContainer;
import com.gentics.mesh.test.MeshOptionsProvider;
import com.gentics.mesh.test.MeshTestContextProvider;

public class SQLServerTestContextProvider extends PreparingDatabaseTestContextProvider<SQLServerDatabaseContainer> implements MeshTestContextProvider {

	static {
		System.setProperty(MeshOptionsProvider.ENV_OPTIONS_PROVIDER_CLASS, SQLServerTestContextProvider.class.getCanonicalName());
	}

	public SQLServerTestContextProvider() {
		super(new SQLServerDatabaseContainer());
	}

	@Override
	public void fillMeshOptions(EnterpriseMeshOptions options) {
		super.fillMeshOptions(options);
		options.getStorageOptions().setConnectionUrl("jdbc:sqlserver://localhost:" + getDatabaseContainer().getMappedPort() + ";databaseName=");
		options.getStorageOptions().setDatabaseName(current);
		options.getStorageOptions().setConnectionUrlExtraParams(";encrypt=false");
        options.getStorageOptions().setConnectionUsername("SA");
		options.getStorageOptions().setConnectionPassword(DatabaseContainer.DEFAULT_PASSWORD);
	}
}
