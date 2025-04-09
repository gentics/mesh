package com.gentics.mesh.enterprise.core;

import java.util.Comparator;

import com.gentics.mesh.core.PreparingDatabaseTestContextProvider;
import com.gentics.mesh.enterprise.dagger.DaggerEnterpriseHibernateMeshComponent;
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
		super(new OracleDatabaseContainer(), DaggerEnterpriseHibernateMeshComponent.builder());
	}

	@Override
	public void fillMeshOptions(HibernateMeshOptions options) {
		super.fillMeshOptions(options);
		options.getStorageOptions().setDatabaseName(DatabaseContainer.DEFAULT_DATABASE);
		options.getStorageOptions().setConnectionUsername(current);
		options.getStorageOptions().setConnectionPassword(DatabaseContainer.DEFAULT_PASSWORD);
	}

	@Override
	public Comparator<String> sortComparator() {
		return (a,b) -> {
			if (a == b) {
				return 0;
			}
			if (a == null) {
				return 1;
			}
			if (b == null) {
				return -1;
			}
			if (!Character.isDigit(a.charAt(0)) && Character.isDigit(b.charAt(0))) {
				return 1;
			}
			if (Character.isDigit(a.charAt(0)) && !Character.isDigit(b.charAt(0))) {
				return -1;
			}
			return a.compareTo(b);
		};
	}
}
