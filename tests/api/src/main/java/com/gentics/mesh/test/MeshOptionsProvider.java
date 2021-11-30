package com.gentics.mesh.test;

import java.util.Set;

import org.reflections.Reflections;
import org.testcontainers.shaded.org.apache.commons.lang.StringUtils;

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
		return spawnProviderInstance(System.getProperty(ENV_OPTIONS_PROVIDER_CLASS), MeshOptionsProvider.class);
	}
	
	//TODO move off to Utils
	@SuppressWarnings("unchecked")
	static <T extends MeshOptionsProvider> T spawnProviderInstance(String className, Class<T> classOfT) {
		
		try {
			Class<? extends T> classToInstantiate;
			if (StringUtils.isBlank(className)) {
				Reflections reflections = new Reflections("com.gentics.mesh");
				Set<Class<? extends T>> classes = reflections.getSubTypesOf(classOfT);
				
				if (classes.size() > 0) {
					classToInstantiate = classes.iterator().next();
				} else {
					throw new IllegalStateException("No implementations of " 
							+ classOfT.getCanonicalName() 
							+ " found in either com.gentics.mesh package or system properties."
							+ " Have you started the test outside of the *tests-runner / *tests-context project?");
				}
			} else {
				classToInstantiate = (Class<? extends T>) MeshOptionsProvider.class.getClassLoader().loadClass(className);
			}
			return (T) classToInstantiate.getConstructor().newInstance();
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}
}
