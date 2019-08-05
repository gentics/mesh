package com.gentics.mesh.dagger;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.etc.config.MeshOptions;

/**
 * Central singleton which provides and keeps track of the dagger mesh dependency context. The stored dagger mesh component exposes various internal data
 * structures. If you use mesh within an application you should use {@link Mesh} instead.
 */
public interface MeshInternal {

	/**
	 * Create a new mesh dagger context if non existed and return it. This method will only create the context once and otherwise return the previously created
	 * context.
	 * 
	 * @param options
	 *            Mesh options which should be injected in the dagger context
	 * 
	 * @return Created or found dagger context
	 */
	static MeshComponent create(MeshOptions options) {
		return DaggerMeshComponent.builder().configuration(options).build();
	}

}
