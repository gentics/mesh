package com.gentics.mesh.test.context;

import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.test.MeshOptionsProvider;

/**
 * A runtime provider of context-specific {@link MeshInstanceProvider}. 
 * The implementations of this interface must be used by setting <code>testContextProviderClass=full.implementation.Class</code> in the system properties.
 * 
 * @author plyhun
 *
 */
public interface MeshTestContextProvider extends MeshOptionsProvider {

	/**
	 * System property key for an {@link MeshTestContextProvider} context-dependent implementation.
	 */
	public static final String ENV_TEST_CONTEXT_PROVIDER_CLASS = "testContextProviderClass";

	/**
	 * Provide the instance provider.
	 * 
	 * @return
	 */
	public MeshInstanceProvider<? extends MeshOptions> getInstanceProvider();
	
	/**
	 * Resolve the provider instance, currently - from the system properties.
	 * 
	 * @return
	 */
	public static MeshTestContextProvider getProvider() {
		try {
			return (MeshTestContextProvider) MeshTestContextProvider.class.getClassLoader().loadClass(System.getProperty(ENV_TEST_CONTEXT_PROVIDER_CLASS)).getConstructor().newInstance();
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}
}
