package com.gentics.mesh.enterprise.core;

import com.gentics.mesh.core.HibernateMeshOptionsProvider;
import com.gentics.mesh.etc.config.HibernateMeshOptions;

public interface HSQLMemoryEnterpriseOptionsProvider extends HibernateMeshOptionsProvider {

	@Override
	default void fillMeshOptions(HibernateMeshOptions meshOptions) {		
		meshOptions.getStorageOptions().setConnectionUsername("sa");
		meshOptions.getStorageOptions().setConnectionPassword("");
	}
}