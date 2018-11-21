package com.gentics.diktyo.wrapper.traversal;

import org.apache.tinkerpop.gremlin.structure.Element;

/**
 * Wrap a traversal in order to provide additional methods which help to deal with the result.
 */
public interface WrappedTraversal<T extends Element> extends TraversalResult<T> {

	//WrappedTraversal<T> traverse(Function<GraphTraversal<PE, E>, GraphTraversal<?, ?>> traverser);

}
