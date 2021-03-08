package com.gentics.mesh.test;

import com.gentics.mesh.etc.config.MeshOptions;

public interface MeshOptionsTypeUnawareContext {
	
	default MeshOptions getOptions() {
		return MeshTestSuite.getOptions();
	}
}
