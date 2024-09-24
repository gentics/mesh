package com.gentics.mesh.core;

import com.gentics.mesh.etc.config.HibernateMeshOptions;
import com.gentics.mesh.test.MeshOptionsProvider;

public interface HibernateMeshOptionsProvider extends MeshOptionsProvider {
	
	void fillMeshOptions(HibernateMeshOptions meshOptions);
}
