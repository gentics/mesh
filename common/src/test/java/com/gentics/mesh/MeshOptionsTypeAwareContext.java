package com.gentics.mesh;

import com.gentics.mesh.etc.config.MeshOptions;

public interface MeshOptionsTypeAwareContext<T extends MeshOptions> {

	/**
	 * Get the options. By default the options are taken from the provider, set in {@link MeshOptionsProvider#ENV_OPTIONS_PROVIDER_CLASS} system property.
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	default T getOptions() {
		try {
			return (T) ((MeshOptionsProvider) getClass().getClassLoader().loadClass(System.getProperty(MeshOptionsProvider.ENV_OPTIONS_PROVIDER_CLASS)).getConstructor().newInstance()).getOptions();
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}
}
