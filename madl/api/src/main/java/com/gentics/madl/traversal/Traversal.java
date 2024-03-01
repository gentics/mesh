package com.gentics.madl.traversal;

import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;

import com.gentics.mesh.util.StreamUtil;

/**
 * The root traversal class. Adapted from a Tinkerpop 2 Pipeline.
 *
 * @param <S>
 *            .
 * @param <T> The type of the objects coming off.
 */
public interface Traversal<S, T> extends Iterator<T>, Iterable<T>, AutoCloseable {

	GraphTraversal<S, T> rawTraversal();

	/**
	 * The serves are an arbitrary filter where the filter criteria is provided by the filterFunction.
	 *
	 * @param predicate
	 *            the filter function of the pipe
	 * @return the extended Pipeline
	 */
	Traversal<S, T> filter(Predicate<T> predicate);

	/**
	 * Will emit the object only if it is in the provided collection.
	 *
	 * @param collection
	 *            the collection to retain
	 * @return the extended Pipeline
	 */
	Traversal<S, T> retain(Iterable<?> collection);

	/**
	 * Emit the respective property of the incoming element.
	 * 
	 * @param key
	 *            the property key
	 * @return the extended Pipeline
	 */
	default PropertyTraversal<?, ?> property(String... key) {
		return new PropertyTraversal<>(rawTraversal().properties(key).map(e -> e.get()));
	}

	/**
	 * Emit the respective property of the incoming element.
	 *
	 * @param <N>
	 *            The type of the property value
	 * @param key
	 *            the property key
	 * @param type
	 *            the property type;
	 * @return the extended Pipeline
	 */
	default <N> PropertyTraversal<?, N> property(Class<N> type, String... key) {
		return new PropertyTraversal<>(rawTraversal().properties(key).map(e -> type.cast(e.get())));
	}

	/**
	 * Return the number of objects iterated through the pipeline.
	 *
	 * @return the number of objects iterated
	 */
	default long count() {
		return StreamUtil.toStream(rawTraversal()).count();
	}

	/**
	 * Return the next object in the pipeline.
	 *
	 */
	@Override
	default T next() {
		return rawTraversal().tryNext().orElseThrow();
	}

	/**
	 * Return the next object in the pipeline.
	 *
	 * @param defaultValue
	 *            The value to be returned if there is no next object in the pipeline.
	 * @return returns the next object in the pipeline, if there are no more objects then defaultValue is returned.
	 */
	default T nextOrDefault(T defaultValue) {
		return rawTraversal().tryNext().orElse(defaultValue);
	}

	/**
	 * Return the next object in the pipeline.
	 *
	 * @param defaultValue
	 *            The value to be returned if there is no next object in the pipeline.
	 * @return returns the next object in the pipeline, if there are no more objects then defaultValue is returned.
	 */
	default T nextOrNull() {
		return nextOrDefault(null);
	}

	/**
	 * Return a list of all the objects in the pipeline.
	 *
	 * @return a list of all the objects
	 */
	default List<T> toList() {
		return rawTraversal().toList();
	}

	/**
	 * Emit the ids of the incoming objects.
	 *
	 * @param <N>
	 *            The type of the id objects.
	 * @return A traversal of the ids.
	 * @since 2.1.0
	 */
	default GraphTraversal<?, ?> id() {
		return rawTraversal().id();
	}

	/**
	 * Emit the ids of the incoming objects, cast to the specified class.
	 *
	 * @param <N>
	 *            The type of the id objects.
	 * @param c
	 *            the class type to cast the ids to.
	 * @return A traversal of the ids.
	 * @since 2.1.0
	 */
	default <N> GraphTraversal<?, N> id(Class<N> c) {
		return rawTraversal().id().map(e -> c.cast(e.get()));
	}

	/**
	 * Stream the content of this traversal.
	 * 
	 * @return
	 */
	default Stream<T> toStream() {
		return StreamUtil.toStream(rawTraversal());
	}

	@Override
	default boolean hasNext() {
		return rawTraversal().hasNext();
	}

	@Override
	default Iterator<T> iterator() {
		return rawTraversal();
	}

	@Override
	default void close() throws Exception {
		rawTraversal().close();
	}
}

