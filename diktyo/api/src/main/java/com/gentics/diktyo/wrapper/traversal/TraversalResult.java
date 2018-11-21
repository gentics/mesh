package com.gentics.diktyo.wrapper.traversal;

import java.util.Iterator;
import java.util.stream.Stream;

import org.apache.tinkerpop.gremlin.structure.Element;

import com.gentics.diktyo.wrapper.element.WrappedElement;

/**
 * Result methods of a traversal.
 * 
 * @param <T>
 */
public interface TraversalResult<T extends Element> {

	/**
	 * Return a stream of elements.
	 * 
	 * @param clazzOfR
	 * @return
	 */
	<R extends WrappedElement<T>> Stream<R> stream(Class<R> clazzOfR);

	/**
	 * Return an iterable of elements.
	 * 
	 * @param clazzOfR
	 * @return
	 */
	default <R extends WrappedElement<T>> Iterable<R> iterable(Class<R> clazzOfR) {
		return () -> stream(clazzOfR).iterator();
	}

	/**
	 * Return an iterator of elements.
	 * 
	 * @param clazzOfR
	 * @return
	 */
	default <R extends WrappedElement<T>> Iterator<R> iterator(Class<R> clazzOfR) {
		return stream(clazzOfR).iterator();
	}

	/**
	 * Return the first found element.
	 * 
	 * @param clazzOfR
	 * @return
	 */
	<R> R next(Class<R> clazzOfR);
}
