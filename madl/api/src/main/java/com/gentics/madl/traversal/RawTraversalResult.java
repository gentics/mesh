package com.gentics.madl.traversal;

import org.apache.tinkerpop.gremlin.structure.Element;

import com.gentics.mesh.madl.frame.ElementFrame;

/**
 * Unwrapped traversal result.
 *
 * @param <E>
 */
public interface RawTraversalResult<E extends Element> extends BaseTraversalResult<E> {

	/**
	 * Frame the traversal and return a type specific result.
	 * 
	 * @param classOfT
	 * @return
	 */
	<T extends ElementFrame> WrappedTraversalResult<T> frameExplicit(Class<T> classOfT);

	/**
	 * Dynamically frame the traversal and return the result.
	 * 
	 * @param classOfT
	 * @return
	 */
	<T extends ElementFrame> WrappedTraversalResult<T> frameDynamic(Class<T> classOfT);

	/**
	 * Return the first found element.
	 * 
	 * @param clazzOfR
	 * @return
	 */
	<R> R next(Class<R> clazzOfR);

}
