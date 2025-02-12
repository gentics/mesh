package com.gentics.mesh.core;

import com.gentics.mesh.etc.config.HibernateMeshOptions;

public interface HSQLMemoryOptionsProvider extends HibernateMeshOptionsProvider {

	@Override
	default void fillMeshOptions(HibernateMeshOptions meshOptions) {		
		meshOptions.getStorageOptions().setConnectionUsername("sa");
		meshOptions.getStorageOptions().setConnectionPassword("");
	}
}