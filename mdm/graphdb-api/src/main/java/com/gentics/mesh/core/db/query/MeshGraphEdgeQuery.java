package com.gentics.mesh.core.db.query;

import java.util.Collection;
import java.util.Optional;

import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;

/**
 * Mesh graph edge query.
 */
public interface MeshGraphEdgeQuery extends MeshMadlGraphQuery<Edge, Optional<? extends Collection<? extends Class<?>>>> {

	/**
	 * Shortcut to the has() predicate, where an operand is an edge direction.
	 * 
	 * @param direction
	 * @param id a vertex, where the edge should point to
	 * @return
	 */
	MeshGraphEdgeQuery directionPointsTo(Direction direction, Object id);

	/**
	 * Shortcut to the has() predicate, where an operand is an IN edge direction.
	 * 
	 * @param direction
	 * @param id a vertex where the edge should come from
	 * @return
	 */
	default MeshGraphEdgeQuery inComesFrom(Object id) {
		return directionPointsTo(Direction.IN, id);
	}

	/**
	 * Shortcut to the has() predicate, where an operand is an OUT edge direction.
	 * 
	 * @param direction
	 * @param id a vertex where the edge should go to
	 * @return
	 */
	default MeshGraphEdgeQuery outGoesTo(Object id) {
		return directionPointsTo(Direction.OUT, id);
	}
}