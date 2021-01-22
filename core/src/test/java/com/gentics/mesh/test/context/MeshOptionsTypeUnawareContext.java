package com.gentics.mesh.test.context;

import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.OrientDBMeshOptions;

public interface MeshOptionsTypeUnawareContext {
	
	default MeshOptions getOptions() {
		return new OrientDBMeshOptions();
	}
}
