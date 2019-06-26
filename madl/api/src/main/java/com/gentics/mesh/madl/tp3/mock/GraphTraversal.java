package com.gentics.mesh.madl.tp3.mock;

import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;

public interface GraphTraversal<S, E> extends Traversal<S, E> {

	GraphTraversal<S, E> has(final String propertyKey, final Object value);

	GraphTraversal<S, Vertex> in(final String... edgeLabels);

	GraphTraversal<S, Vertex> out(final String... edgeLabels);

	GraphTraversal<S, Vertex> outV();

	GraphTraversal<S, Vertex> inV();

	GraphTraversal<S, Edge> outE(final String... edgeLabels);

	GraphTraversal<S, Edge> inE(final String... edgeLabels);

	GraphTraversal<S, Long> count();

}
