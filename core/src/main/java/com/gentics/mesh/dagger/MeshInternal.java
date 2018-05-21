package com.gentics.mesh.dagger;

import java.util.concurrent.atomic.AtomicReference;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.etc.config.MeshOptions;

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
	 * @param options
	 *            Mesh options which should be injected in the dagger context
	 * 
	 * @return
	 */
	static MeshComponent create(MeshOptions options) {
		if (applicationComponent.get() == null) {
			applicationComponent.set(DaggerMeshComponent.builder().configuration(options).build());
		}
		return applicationComponent.get();

	}

	/**
	 * Clear the reference to the dagger mesh component. This is useful if you want to start a new dagger context (e.g. for unit tests).
	 */
	static void clear() {
		applicationComponent.set(null);
	}

	/**
	 * Return the created context.
	 * 
	 * @return Created dagger context or null if none has been created previously
	 */
	@SuppressWarnings("unchecked")
	static <T extends MeshComponent> T get() {
		return (T) applicationComponent.get();
	}

	/**
	 * Set the component (useful for testing)
	 * 
	 * @param component
	 */
	static void set(MeshComponent component) {
		applicationComponent.set(component);
	}

}
