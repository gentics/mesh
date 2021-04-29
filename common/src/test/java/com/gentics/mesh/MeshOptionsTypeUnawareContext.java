package com.gentics.mesh;

import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.OrientDBMeshOptions;

public interface MeshOptionsTypeUnawareContext {
	
	/**
	 * Get the options. By default the options are taken from the provider, set in {@link MeshOptionsProvider#ENV_OPTIONS_PROVIDER_CLASS} system property.
	 * 
	 * @return
	 */
	default MeshOptions getOptions() {
		try {
			return ((MeshOptionsProvider) getClass().getClassLoader().loadClass(System.getProperty("optionsProviderClass")).getConstructor().newInstance()).getOptions();
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}
}
