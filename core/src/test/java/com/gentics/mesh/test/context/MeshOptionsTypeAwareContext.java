package com.gentics.mesh.test.context;

import com.gentics.mesh.etc.config.MeshOptions;

public interface MeshOptionsTypeAwareContext<T extends MeshOptions> {

	T getOptions();
}
