package com.gentics.mesh.core.endpoint.admin.consistency;

import com.gentics.mesh.core.data.MeshVertex;

/**
 * Expected edge definition for the consistency check.
 */
@FunctionalInterface
public interface Edge {
	<N extends MeshVertex> N follow(MeshVertex v, String label, Class<N> clazz);
}
