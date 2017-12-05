package com.gentics.mesh.core.verticle.admin.consistency;

import com.gentics.mesh.core.data.MeshVertex;

@FunctionalInterface
public interface Edge {
	<N extends MeshVertex> N follow(MeshVertex v, String label, Class<N> clazz);
}
