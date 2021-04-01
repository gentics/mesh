package com.gentics.mesh.test;

import com.gentics.mesh.etc.config.MeshOptions;

/**
 * This interface may be used by the implementor unit test classes, 
 * that need an implementation of {@link MeshOptions} without a care of an actual implementation class,
 * i.e for the context-independent business logic tests.
 * 
 * @author plyhun
 *
 */
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
