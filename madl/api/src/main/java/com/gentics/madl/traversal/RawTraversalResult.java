package com.gentics.madl.traversal;

import com.gentics.mesh.madl.frame.ElementFrame;
import com.gentics.mesh.madl.tp3.mock.Element;

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
