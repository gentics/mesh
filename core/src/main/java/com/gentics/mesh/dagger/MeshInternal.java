package com.gentics.mesh.dagger;

import java.util.concurrent.atomic.AtomicReference;

import com.gentics.mesh.Mesh;

/**
 * Central singleton which provides and keeps track of the dagger mesh dependency context. The stored dagger mesh component exposes various internal data
 * structures. If you use mesh within an application you should use {@link Mesh} instead.
 */
public interface MeshInternal {

	static AtomicReference<MeshComponent> applicationComponent = new AtomicReference<>(null);

	/**
	 * Create a new mesh dagger context if non existed and return it. This method will only create the context once and otherwise return the previously created
	 * context.
	 * 
	 * @return
	 */
	static MeshComponent create() {

		if (applicationComponent.get() == null) {
			applicationComponent.set(DaggerMeshComponent.builder().build());
		}
		return applicationComponent.get();

	}

	/**
	 * Return the created context.
	 * 
	 * @return Created dagger context or null if none has been created previously
	 */
	static <T extends MeshComponent> T get() {
		return (T) applicationComponent.get();
	}

}
