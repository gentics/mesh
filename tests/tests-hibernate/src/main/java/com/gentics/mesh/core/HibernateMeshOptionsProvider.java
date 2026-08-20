package com.gentics.mesh.core;

import com.gentics.mesh.etc.config.HibernateMeshOptions;
import com.gentics.mesh.test.MeshOptionsProvider;

/**
 * An extension to the {@link MeshOptionsProvider} to customize options class and instance.
 */
public interface HibernateMeshOptionsProvider extends MeshOptionsProvider {

	public static final String ENV_OPTIONS_CLASS = "optionsClass";
	
	void fillMeshOptions(HibernateMeshOptions meshOptions);
}
