package com.gentics.mesh.test;

import com.gentics.mesh.etc.config.MeshOptions;

/**
 * A runtime provider of context-specific {@link MeshOptions}. 
 * The implementations of this interface must be used by setting <code>optionsProviderClass=full.implementation.Class</code> in the system properties.
 * 
 * @author plyhun
 *
 */
public interface MeshOptionsProvider {

	/**
	 * System property key for an {@link MeshOptionsProvider} context-dependent implementation.
	 */
	public static final String ENV_OPTIONS_PROVIDER_CLASS = "optionsProviderClass";

	/**
	 * Provide the options instance.
	 * 
	 * @return
	 */
	public MeshOptions getOptions();
	
	/**
	 * Resolve the provider instance, currently - from the system properties.
	 * 
	 * @return
	 */
	public static MeshOptionsProvider getProvider() {
		try {
			return (MeshOptionsProvider) MeshOptionsProvider.class.getClassLoader().loadClass(System.getProperty(ENV_OPTIONS_PROVIDER_CLASS)).getConstructor().newInstance();
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}
}
