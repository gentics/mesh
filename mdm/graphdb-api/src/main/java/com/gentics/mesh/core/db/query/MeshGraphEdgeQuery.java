package com.gentics.mesh.core.db.query;

import java.util.Collection;
import java.util.Optional;

import org.apache.tinkerpop.gremlin.structure.Edge;

/**
 * Mesh graph edge query.
 */
public interface MeshGraphEdgeQuery extends MeshMadlGraphQuery<Edge, Optional<? extends Collection<? extends Class<?>>>> {

}