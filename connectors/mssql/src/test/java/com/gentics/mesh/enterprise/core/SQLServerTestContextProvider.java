package com.gentics.mesh.enterprise.core;

import java.util.Comparator;

import com.gentics.mesh.core.PreparingDatabaseTestContextProvider;
import com.gentics.mesh.enterprise.dagger.DaggerEnterpriseHibernateMeshComponent;
import com.gentics.mesh.enterprise.test.docker.SQLServerDatabaseContainer;
import com.gentics.mesh.etc.config.HibernateMeshOptions;
import com.gentics.mesh.test.MeshOptionsProvider;
import com.gentics.mesh.test.MeshTestContextProvider;
import com.gentics.mesh.test.docker.DatabaseContainer;

public class SQLServerTestContextProvider extends PreparingDatabaseTestContextProvider<SQLServerDatabaseContainer> implements MeshTestContextProvider {

	static {
		System.setProperty(MeshOptionsProvider.ENV_OPTIONS_PROVIDER_CLASS, SQLServerTestContextProvider.class.getCanonicalName());
	}

	public SQLServerTestContextProvider() {
		super(new SQLServerDatabaseContainer(), DaggerEnterpriseHibernateMeshComponent.builder());
	}

	@Override
	public void fillMeshOptions(HibernateMeshOptions options) {
		super.fillMeshOptions(options);
		options.getStorageOptions().setDatabaseName(current);
		options.getStorageOptions().setConnectionUsername("SA");
		options.getStorageOptions().setConnectionPassword(DatabaseContainer.DEFAULT_PASSWORD);
	}

	@Override
	public Comparator<String> sortComparator() {
		return SQLServerTestUtils.SORT_COMPARATOR;
	}
}
