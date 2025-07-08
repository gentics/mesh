package com.gentics.mesh.enterprise.core;

import java.util.Comparator;

import com.gentics.mesh.core.PreparingDatabaseTestContextProvider;
import com.gentics.mesh.enterprise.dagger.DaggerEnterpriseHibernateMeshComponent;
import com.gentics.mesh.enterprise.test.docker.PostgresDatabaseContainer;
import com.gentics.mesh.etc.config.HibernateMeshOptions;
import com.gentics.mesh.test.MeshOptionsProvider;
import com.gentics.mesh.test.MeshTestContextProvider;
import com.gentics.mesh.test.docker.DatabaseContainer;

public class PostgresTestContextProvider extends PreparingDatabaseTestContextProvider<PostgresDatabaseContainer> implements MeshTestContextProvider {

	static {
		System.setProperty(MeshOptionsProvider.ENV_OPTIONS_PROVIDER_CLASS, PostgresTestContextProvider.class.getCanonicalName());
	}

	public PostgresTestContextProvider() {
		super(new PostgresDatabaseContainer(), DaggerEnterpriseHibernateMeshComponent.builder());
	}

	@Override
	public void fillMeshOptions(HibernateMeshOptions options) {
		super.fillMeshOptions(options);
		options.getStorageOptions().setConnectionUrlExtraParams("autosave=always&currentSchema=" + current);
		options.getStorageOptions().setDatabaseName(DatabaseContainer.DEFAULT_DATABASE);
		options.getStorageOptions().setConnectionUsername(DatabaseContainer.DEFAULT_USERNAME);
		options.getStorageOptions().setConnectionPassword(DatabaseContainer.DEFAULT_PASSWORD);
	}

	@Override
	public Comparator<String> sortComparator() {
		return PostgresTestUtils.SORT_COMPARATOR;
	}
}
