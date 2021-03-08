package com.gentics.mesh.test;

import com.gentics.mesh.etc.config.MeshOptions;

public interface MeshOptionsTypeAwareContext<T extends MeshOptions> {

	T getOptions();
}
