package com.gentics.mesh.dagger;

/**
 * Builder for the dagger mesh component.
 */
public interface MeshBuilderFactory {

	/**
	 * Return the builder for the {@link MeshComponent}.
	 * 
	 * @return
	 */
	MeshComponent.Builder getBuilder();
}
