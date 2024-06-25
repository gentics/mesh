package com.gentics.mesh.enterprise.etc.config;

import com.gentics.mesh.enterprise.core.EnterpriseMeshOptionsProvider;

public interface HSQLMemoryEnterpriseOptionsProvider extends EnterpriseMeshOptionsProvider {

	@Override
	default void fillMeshOptions(EnterpriseMeshOptions meshOptions) {		
		meshOptions.getStorageOptions().setConnectionUrl("jdbc:hsqldb:mem:tsg");
		meshOptions.getStorageOptions().setConnectionUsername("sa");
		meshOptions.getStorageOptions().setConnectionPassword("");
	}
}