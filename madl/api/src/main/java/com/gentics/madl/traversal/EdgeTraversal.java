package com.gentics.madl.traversal;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Edge;

import com.gentics.mesh.madl.frame.ElementFrame;
import com.gentics.mesh.util.StreamUtil;


public interface EdgeTraversal<S, M extends Edge> extends FramedTraversal<S, M> {

	/**
	 * Check if the element has a property with provided key.
	 *
	 * @param key
	 *            the property key to check
	 * @return the extended Pipeline
	 */
	default EdgeTraversal<S, M> has(String key) {
		return new EdgeTraversalImpl<>(getGraph(), rawTraversal().has(key));
	}

	/**
	 * Add an IdFilterPipe, LabelFilterPipe, or PropertyFilterPipe to the end of the Pipeline. If the incoming element has the provided key/value as check with
	 * .equals(), then let the element pass. If the key is id or label, then use respect id or label filtering.
	 *
	 * @param key
	 *            the property key to check
	 * @param value
	 *            the object to filter on (in an OR manner)
	 * @return the extended Pipeline
	 */
	default EdgeTraversal<S, M> has(String key, Object value) {
		return new EdgeTraversalImpl<>(getGraph(), rawTraversal().has(key, value));
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
	default EdgeTraversal<S, M> has(String[] keys, Object[] values) {
		GraphTraversal<S, M> rawTraversal = rawTraversal();
		for (int i = 0; i < keys.length; i++) {
			rawTraversal = rawTraversal.has(keys[i], values[i]);
		}
		return new EdgeTraversalImpl<>(getGraph(), rawTraversal);
	}

	/**
	 * Add an IdFilterPipe, LabelFilterPipe, or PropertyFilterPipe to the end of the Pipeline. If the incoming element has the provided key/value as check with
	 * .equals(), then let the element pass. If the key is id or label, then use respect id or label filtering.
	 *
	 * @param key
	 *            the property key to check
	 * @param compareToken
	 *            the comparison to use
	 * @param value
	 *            the object to filter on
	 * @return the extended Pipeline
	 */
	default EdgeTraversal<S, M> has(String key, P predicate) {
		return new EdgeTraversalImpl<>(getGraph(), rawTraversal().has(key, predicate));
	}

	/**
	 * If the incoming edge has the provided ferma_type property that is checked against the given class, then let the element pass.
	 * 
	 * @param clazz
	 *            the class that should be used for filtering
	 * @return the extended Pipeline
	 */
	default EdgeTraversal<S, M> has(Class<?> clazz) {
		return new EdgeTraversalImpl<>(getGraph(), rawTraversal().has(ElementFrame.TYPE_RESOLUTION_KEY, clazz.getSimpleName()));
	}

	/**
	 * Check if the element does not have a property with provided key.
	 *
	 * @param key
	 *            the property key to check
	 * @return the extended Pipeline
	 */
	default EdgeTraversal<S, M> hasNot(String key) {
		return new EdgeTraversalImpl<>(getGraph(), rawTraversal().hasNot(key));
	}

	/**
	 * Add an IdFilterPipe, LabelFilterPipe, or PropertyFilterPipe to the end of the Pipeline. If the incoming element has the provided key/value as check with
	 * .equals(), then filter the element. If the key is id or label, then use respect id or label filtering.
	 *
	 * @param key
	 *            the property key to check
	 * @param value
	 *            the objects to filter on (in an OR manner)
	 * @return the extended Pipeline
	 */
	default EdgeTraversal<S, M> hasNot(String key, Object value) {
		return new EdgeTraversalImpl<>(getGraph(), rawTraversal().has(key, P.neq(value)));
	}

	/**
	 * Add an InVertexPipe to the end of the Pipeline. Emit the head vertex of the incoming edge.
	 *
	 * @return the extended Pipeline
	 */
	default VertexTraversal<S, ?> inV() {
		return new VertexTraversalImpl<>(getGraph(), rawTraversal().inV());
	}

	/**
	 * Add an OutVertexPipe to the end of the Pipeline. Emit the tail vertex of the incoming edge.
	 *
	 * @return the extended Pipeline
	 */
	default VertexTraversal<S, ?> outV() {
		return new VertexTraversalImpl<>(getGraph(), rawTraversal().outV());
	}

	/**
	 * Get the next object emitted from the pipeline. If no such object exists, then a NoSuchElementException is thrown.
	 * 
	 * @param <T>
	 *            The type to frame the element as.
	 * @param kind
	 *            The type of frame for the element.
	 * @return the next emitted object
	 */
	default <T> T next(Class<T> kind) {
		return frameElement(rawTraversal().has(ElementFrame.TYPE_RESOLUTION_KEY, kind.getSimpleName()).next(), kind);
	}

	/**
	 * Get the next object emitted from the pipeline. If no such object exists, then a NoSuchElementException is thrown.
	 *
	 * This will bypass the default type resolution and use the untyped resolver instead. This method is useful for speeding up a look up when type resolution
	 * isn't required.
	 *
	 * @param <T>
	 *            The type to frame the element as.
	 * @param kind
	 *            The type of frame for the element.
	 * @return the next emitted object
	 */
	default <T> T nextExplicit(Class<T> kind) {
		return frameElementExplicit(rawTraversal().next(), kind);
	}

	/**
	 * Return an iterator of framed elements.
	 *
	 * This will bypass the default type resolution and use the untyped resolver instead. This method is useful for speeding up a look up when type resolution
	 * isn't required.
	 *
	 * @param <T>
	 *            The type to frame the element as.
	 * @param kind
	 *            The kind of framed elements to return.
	 * @return An iterator of framed elements.
	 */
	default <T> Iterable<T> frameExplicit(Class<? extends T> kind) {
		return StreamUtil.toIterable(frameExplicit(rawTraversal(), kind));
	}

	/**
	 * Return a list of all the objects in the pipeline.
	 * 
	 * @param <T>
	 *            The type to frame the element as.
	 * @param kind
	 *            The kind of framed elements to return.
	 * @return a list of all the objects
	 */
	default <T> List<? extends T> toList(Class<T> kind) {
		return (List<? extends T>) StreamUtil.toStream(frame(rawTraversal().has(ElementFrame.TYPE_RESOLUTION_KEY, kind.getSimpleName()), kind)).collect(Collectors.toList());
	}

	/**
	 * Return a list of all the objects in the pipeline.
	 *
	 * This will bypass the default type resolution and use the untyped resolver instead. This method is useful for speeding up a look up when type resolution
	 * isn't required.
	 *
	 * @param <T>
	 *            The type to frame the element as.
	 * @param kind
	 *            The kind of framed elements to return.
	 * @return a list of all the objects
	 */
	default <T> List<? extends T> toListExplicit(Class<T> kind) {
		return (List<? extends T>) StreamUtil.toStream(frameExplicit(rawTraversal(), kind)).collect(Collectors.toList());
	}

	/**
	 * Add an LabelPipe to the end of the Pipeline. Emit the label of the incoming edge.
	 *
	 * @return the extended Pipeline
	 */
	default GraphTraversal<S, String> label() {
		return rawTraversal().label();
	}

	@Override
	EdgeTraversal<S, M> filter(Predicate<M> filterFunction);

	@Override
	default EdgeTraversal<S, M> retain(Iterable<?> collection) {
		return new EdgeTraversalImpl<>(getGraph(), rawTraversal().is(P.within(StreamUtil.toStream(collection).collect(Collectors.toSet()))));
	}

	/**
	 * Add an OrFilterPipe to the end the Pipeline. Will only emit the object if one or more of the provides pipes yields an object. The provided pipes are
	 * provided the object as their starts.
	 *
	 * @param pipes
	 *            the internal pipes of the OrFilterPipe
	 * @return the extended Pipeline
	 */
	default EdgeTraversal<S, M> or(Traversal<S, M>... pipes) {
		return new EdgeTraversalImpl<>(getGraph(), rawTraversal().or(Arrays.stream(pipes).map(Traversal::rawTraversal).collect(Collectors.toList()).toArray(new GraphTraversal[pipes.length])));
	}

	/**
	 * Remove every element at the end of this Pipeline.
	 */
	default void removeAll() {
		rawTraversal().forEachRemaining(Edge::remove);
	}

	/**
	 * Get the next object emitted from the pipeline. If no such object exists, then a the default value is returned.
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
}
