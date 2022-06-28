package com.gentics.mesh.etc.config.env;

import com.gentics.mesh.etc.config.MeshOptions;

/**
 * This interface may be used by the implementor unit test classes, 
 * that need an implementation of {@link MeshOptions} with keeping an actual implementation type,
 * e.g. for context depentent tests.
 * 
 * @author plyhun
 *
 * @param <T>
 */
public interface MeshOptionsContext<T extends MeshOptions> {

	/**
	 * Get the options. By default the options are taken from the provider, set in {@link MeshOptionsProvider#ENV_OPTIONS_PROVIDER_CLASS} system property.
	 * 
	 * @return
	 */
	T getOptions();
}
