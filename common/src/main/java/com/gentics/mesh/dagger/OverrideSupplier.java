package com.gentics.mesh.dagger;

import com.gentics.mesh.Mesh;

/**
 * A supplier function for the support of overridden Dagger dependencies.
 * 
 * @author plyhun
 *
 */
@FunctionalInterface
public interface OverrideSupplier<T> {

	/**
	 * Get the dependency, initializing its internals with Mesh instance, if required.
	 * 
	 * @param mesh
	 * @return
	 */
	T get(Mesh mesh);
}
