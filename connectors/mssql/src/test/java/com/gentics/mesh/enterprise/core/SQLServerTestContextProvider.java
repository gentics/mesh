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
		Comparator<Character> charComparator = (a, b) -> {
			if (a == b) {
				return 0;
			}
			if (a == null) {
				return -1;
			}
			if (b == null) {
				return 1;
			}
			if (!Character.isDigit(a) && Character.isDigit(b)) {
				return 1;
			}
			if (Character.isDigit(a) && !Character.isDigit(b)) {
				return -1;
			}
			return Character.valueOf(Character.toLowerCase(a)).compareTo(Character.toLowerCase(b));
		};
		return (a,b) -> {
			if (a == b) {
				return 0;
			}
			if (a == null) {
				return -1;
			}
			if (b == null) {
				return 1;
			}
			int lenA = a.length();
			int lenB = b.length();
			for (int i = 0; i < Math.max(lenA, lenB); i++) {
				Character ca = i < lenA ? a.charAt(i) : null;
				Character cb = i < lenB ? b.charAt(i) : null;
				int res = charComparator.compare(ca, cb);
				if (res != 0) {
					return res;
				}
			}
			return 0;
		};
	}
}
