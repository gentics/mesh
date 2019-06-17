package com.gentics.mesh.madl.tp3.mock;

public interface GraphTraversal<S, E> extends Traversal<S, E> {

	GraphTraversal<S, E> has(final String propertyKey, final Object value);
}
