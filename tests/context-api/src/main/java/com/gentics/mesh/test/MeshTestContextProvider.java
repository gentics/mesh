package com.gentics.mesh.test;

import java.util.Comparator;

import com.gentics.mesh.etc.config.MeshOptions;

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
		return MeshOptionsProvider.spawnProviderInstance(System.getProperty(ENV_TEST_CONTEXT_PROVIDER_CLASS), MeshTestContextProvider.class);
	}

	/**
	 * Get a sorting mode, that current Mesh instance or its database implements. The default value does nothing, assuming everything is ordered.
	 * 
	 * @return
	 */
	default Comparator<String> sortComparator() {
		return (a,b) -> {
			if (a == b) {
				return 0;
			}
			if (a == null) {
				return -1;
			}
			if (b == null) {
				return 1;
			}
			if (!Character.isDigit(a.charAt(0)) && Character.isDigit(b.charAt(0))) {
				return -1;
			}
			if (Character.isDigit(a.charAt(0)) && !Character.isDigit(b.charAt(0))) {
				return 1;
			}
			return a.compareTo(b);
		};
	}
}
