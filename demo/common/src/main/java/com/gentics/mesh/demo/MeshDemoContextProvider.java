package com.gentics.mesh.demo;

import com.gentics.mesh.test.MeshOptionsProvider;

/**
 * Demo context provider for the tests.
 * 
 * @author plyhun
 *
 */
public interface MeshDemoContextProvider extends MeshOptionsProvider {
	/**
	 * System property key for an {@link MeshDemoContextProvider} context-dependent implementation.
	 */
	public static final String ENV_DEMO_CONTEXT_PROVIDER_CLASS = "demoContextProviderClass";

	/**
	 * Provide the demo dumper.
	 * 
	 * @return
	 */
	public DemoDumper getDumper();
	
	/**
	 * Resolve the provider instance, currently - from the system properties.
	 * 
	 * @return
	 */
	public static MeshDemoContextProvider getProvider() {
		return MeshOptionsProvider.spawnProviderInstance(System.getProperty(ENV_DEMO_CONTEXT_PROVIDER_CLASS), MeshDemoContextProvider.class);
	}
}
