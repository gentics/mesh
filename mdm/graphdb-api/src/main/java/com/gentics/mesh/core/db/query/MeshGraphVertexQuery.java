package com.gentics.mesh.core.db.query;

import java.util.Optional;

import org.apache.tinkerpop.gremlin.structure.Vertex;

import com.gentics.mesh.core.rest.common.ContainerType;

/**
 * Mesh graph vertex query.
 */
public interface MeshGraphVertexQuery extends MeshMadlGraphQuery<Vertex, Optional<ContainerType>> {

}