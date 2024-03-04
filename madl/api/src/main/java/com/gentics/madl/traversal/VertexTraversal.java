package com.gentics.madl.traversal;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import com.gentics.mesh.util.StreamUtil;
import com.syncleus.ferma.VertexFrame;

public interface VertexTraversal<S, M extends Vertex> extends FramedTraversal<S, M> {

	/**
	 * Check if the element has a property with provided key.
	 *
	 * @param key
	 *            the property key to check
	 * @return the extended Pipeline
	 */
	default VertexTraversal<S, M> has(String key) {
		return new VertexTraversalImpl<>(getGraph(), rawTraversal().has(key));
	}

	/**
	 * If the incoming element has the provided key/value as check with .equals(), then let the element pass.
	 *
	 * @param key
	 *            the property key to check.
	 * @param value
	 *            the object to filter on (in an OR manner)
	 * @return the extended Pipeline
	 */
	default VertexTraversal<S, M> has(String key, Object value) {
		return new VertexTraversalImpl<>(getGraph(), rawTraversal().has(key, value));
	}

	/**
	 * If the incoming element has the provided key/value as check with .equals(), then let the element pass.
	 *
	 * @param key
	 *            the property key to check
	 * @param compareToken
	 *            the comparison to use
	 * @return the extended Pipeline
	 */
	default VertexTraversal<S, M> has(String key, P<?> predicate) {
		return new VertexTraversalImpl<>(getGraph(), rawTraversal().has(key, predicate));
	}

	/**
	 * If the incoming vertex has the provided ferma_type property that is checked against the given class, then let the element pass.
	 *
	 * @param clazz
	 *            the class to check against
	 * @return the extended Pipeline
	 */
	default VertexTraversal<S, M> has(Class<?> clazz) {
		return new VertexTraversalImpl<>(getGraph(), rawTraversal().hasLabel(clazz.getSimpleName()));
	}

	/**
	 * If the incoming element has all the provided key/values as check with .equals(), then filter the element.
	 *
	 * @param keys
	 *            the property keys to check
	 * @param values
	 *            the objects to filter on (in an AND manner)
	 * @return the extended Pipeline
	 */
	default VertexTraversal<S, M> has(String[] keys, Object[] values) {
		GraphTraversal<S, M> rawTraversal = rawTraversal();
		for (int i = 0; i < keys.length; i++) {
			rawTraversal = rawTraversal.has(keys[i], P.test((a, b) -> {
				boolean rawEquals = Objects.equals(a, b); 
				if (rawEquals) {
					return rawEquals;
				}
				if (Collection.class.isInstance(b)) {
					if (Collection.class.isInstance(a)) {
						return CollectionUtils.disjunction(Collection.class.cast(b), Collection.class.cast(a)).size() == 0;
					} else {
						return Collection.class.cast(b).stream().anyMatch(bb -> Objects.equals(bb, a));
					}
				} else {
					if (Collection.class.isInstance(a)) {
						return Collection.class.cast(a).stream().anyMatch(aa -> Objects.equals(aa, b));
					} else {
						return rawEquals;
					}
				}
			}, values[i]));
		}
		return new VertexTraversalImpl<>(getGraph(), rawTraversal);
	}

	/**
	 * If the incoming element has the provided key/value as check with .equals(), then filter the element.
	 *
	 * @param key
	 *            the property key to check
	 * @param value
	 *            the objects to filter on (in an OR manner)
	 * @return the extended Pipeline
	 */
	default VertexTraversal<S, M> hasNot(String key, Object value) {
		return new VertexTraversalImpl<>(getGraph(), rawTraversal().has(key, P.neq(value)));
	}
	/**
	 * If the incoming element has no provided key, then filter the element.
	 *
	 * @param key
	 *            the property key to check
	 * @param value
	 *            the objects to filter on (in an OR manner)
	 * @return the extended Pipeline
	 */
	default VertexTraversal<S, M> hasNot(String key) {
		return new VertexTraversalImpl<>(getGraph(), rawTraversal().hasNot(key));
	}

	/**
	 * If the incoming element has the following type label, then filter the element.
	 *
	 * @param label
	 *            the type label to check
	 * @param value
	 *            the objects to filter on (in an OR manner)
	 * @return the extended Pipeline
	 */
	default VertexTraversal<S, M> hasLabel(String label) {
		return new VertexTraversalImpl<>(getGraph(), rawTraversal().hasLabel(label));
	}

	/**
	 * Emit the adjacent outgoing vertices of the incoming vertex.
	 *
	 * @param labels
	 *            the edge labels to traverse
	 * @return the extended Pipeline
	 */
	default VertexTraversal<S, ?> out(String... labels) {
		return new VertexTraversalImpl<>(getGraph(), rawTraversal().out(labels));
	}

	/**
	 * Emit the adjacent incoming vertices for the incoming vertex.
	 *
	 * @param labels
	 *            the edge labels to traverse
	 * @return the extended Pipeline
	 */
	default VertexTraversal<S, ?> in(String... labels) {
		return new VertexTraversalImpl<>(getGraph(), rawTraversal().in(labels));
	}

	/**
	 * Emit the outgoing edges for the incoming vertex.
	 *
	 * @param labels
	 *            the edge labels to traverse
	 * @return the extended Pipeline
	 */
	default EdgeTraversal<S, ?> outE(String... labels) {
		return new EdgeTraversalImpl<>(getGraph(), rawTraversal().outE(labels));
	}

	/**
	 * Emit the incoming edges for the incoming vertex.
	 *
	 * @param labels
	 *            the edge labels to traverse
	 * @return the extended Pipeline
	 */
	default EdgeTraversal<S, ?> inE(String... labels) {
		return new EdgeTraversalImpl<>(getGraph(), rawTraversal().inE(labels));
	}

	/**
	 * Get the next object emitted from the pipeline. If no such object exists, then a NoSuchElementException is thrown.
	 * 
	 * @param <N>
	 *            The type used to frame the element
	 * @param kind
	 *            The type of frame for the element.
	 * @return the next emitted object
	 */
	default <N> N next(Class<N> kind) {
		return frameElement(rawTraversal().hasLabel(kind.getSimpleName()).next(), kind);
	}

	/**
	 * Get the next object emitted from the pipeline. If no such object exists, then a NoSuchElementException is thrown.
	 *
	 * This will bypass the default type resolution and use the untyped resolver instead. This method is useful for speeding up a look up when type resolution
	 * isn't required.
	 *
	 * @param <N>
	 *            The type used to frame the element
	 * @param kind
	 *            The type of frame for the element.
	 * @return the next emitted object
	 */
	default <N> N nextExplicit(Class<N> kind) {
		return frameElementExplicit(rawTraversal().next(), kind);
	}

	/**
	 * Get the next object emitted from the pipeline. If no such object exists, then the default value is returned.
	 * 
	 * @param <N>
	 *            The type used to frame the element
	 * @param kind
	 *            The type of frame for the element.
	 * @param defaultValue
	 *            The object to return if no next object exists.
	 * @return the next emitted object
	 */
	default <N> N nextOrDefault(Class<N> kind, N defaultValue) {
		return rawTraversal().hasLabel(kind.getSimpleName()).tryNext()
				.map(element -> frameElement(element, kind))
				.orElse(defaultValue);
	}

	/**
	 * Get the next object emitted from the pipeline. If no such object exists, then a null is returned.
	 * 
	 * @param <N>
	 *            The type used to frame the element
	 * @param kind
	 *            The type of frame for the element.
	 * @param defaultValue
	 *            The object to return if no next object exists.
	 * @return the next emitted object
	 */
	default <N> N nextOrNull(Class<N> kind) {
		return nextOrDefault(kind, null);
	}

	/**
	 * Get the next object emitted from the pipeline. If no such object exists, then the default value is returned.
	 *
	 * This will bypass the default type resolution and use the untyped resolver instead. This method is useful for speeding up a look up when type resolution
	 * isn't required.
	 *
	 * @param <N>
	 *            The type used to frame the element
	 * @param kind
	 *            The type of frame for the element.
	 * @param defaultValue
	 *            The object to return if no next object exists.
	 * @return the next emitted object
	 */
	default <N> N nextOrDefaultExplicit(Class<N> kind, N defaultValue) {
		return rawTraversal().tryNext()
				.map(element -> frameElementExplicit(element, kind))
				.orElse(defaultValue);
	}

	/**
	 * Get the next object emitted from the pipeline. If no such object exists, then a null is returned.
	 *
	 * This will bypass the default type resolution and use the untyped resolver instead. This method is useful for speeding up a look up when type resolution
	 * isn't required.
	 *
	 * @param <N>
	 *            The type used to frame the element
	 * @param kind
	 *            The type of frame for the element.
	 * @param defaultValue
	 *            The object to return if no next object exists.
	 * @return the next emitted object
	 */
	default <N> N nextOrNullExplicit(Class<N> kind) {
		return nextOrDefaultExplicit(kind, null);
	}

	/**
	 * Return an iterator of framed elements.
	 * 
	 * @param <N>
	 *            The type used to frame the element
	 * @param kind
	 *            The kind of framed elements to return.
	 * @return An iterator of framed elements.
	 */
	default <N> Iterable<N> frame(Class<N> kind) {
		return StreamUtil.toIterable(frame(rawTraversal().hasLabel(kind.getSimpleName()), kind));
	}

	/**
	 * Return an iterator of framed elements.
	 *
	 * This will bypass the default type resolution and use the untyped resolver instead. This method is useful for speeding up a look up when type resolution
	 * isn't required.
	 *
	 * @param <N>
	 *            The type used to frame the element
	 * @param kind
	 *            The kind of framed elements to return.
	 * @return An iterator of framed elements.
	 */
	default <N> Iterable<N> frameExplicit(Class<N> kind) {
		return StreamUtil.toIterable(frameExplicit(rawTraversal(), kind));
	}

	@Override
	VertexTraversal<S, M> filter(Predicate<M> filterFunction);

	/**
	 * Will emit the object only if it is in the provided collection.
	 *
	 * @param vertices
	 *            the collection to retain
	 * @return the extended Pipeline
	 */
	default VertexTraversal<S, M> retain(VertexFrame... vertices) {
		return new VertexTraversalImpl<>(getGraph(), rawTraversal().is(P.within(Arrays.asList(vertices))));
	}

	@Override
	default VertexTraversal<S, M> retain(Iterable<?> collection) {
		return new VertexTraversalImpl<>(getGraph(), rawTraversal().is(P.within(StreamUtil.toStream(collection).collect(Collectors.toSet()))));
	}

	/**
	 * Remove every element at the end of this Pipeline.
	 */
	default void removeAll() {
		rawTraversal().forEachRemaining(Vertex::remove);
	}
}
