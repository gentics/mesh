package com.gentics.mesh.dagger;

/**
 * A builder factory for Mesh Hibernate.
 * 
 * @author plyhun
 *
 */
public class HibMeshBuilderFactory implements MeshBuilderFactory {
	public MeshComponent.Builder getBuilder() {
		return DaggerHibernateMeshComponent.builder();
	}
}
