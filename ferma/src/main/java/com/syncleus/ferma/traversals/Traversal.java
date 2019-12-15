/**
 * Copyright 2004 - 2016 Syncleus, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * Part or all of this source file was forked from a third-party project, the details of which are listed below.
 *
 * Source Project: Totorom
 * Source URL: https://github.com/BrynCooke/totorom
 * Source License: Apache Public License v2.0
 * When: November, 20th 2014
 */
package com.syncleus.ferma.traversals;

import java.util.Iterator;
import java.util.List;

import com.tinkerpop.gremlin.java.GremlinPipeline;

/**
 * The root traversal class. Wraps a Tinkerpop {@link GremlinPipeline}
 *
 * @param <T>
 *            The type of the objects coming off the pipe.
 * @param <C>
 *            The cap of the current pipe.
 * @param <S>
 *            The SideEffect of the current pipe.
 * @param <M>
 *            The current marked type for the current pipe.
 */
@Deprecated
public interface Traversal<T, C, S, M> extends Iterator<T>, Iterable<T> {

	/**
	 * Traverse over all the vertices in the graph.
	 * 
	 * @return The traversal representing all vertices in the graph.
	 */
	VertexTraversal<?, ?, M> v();

	/**
	 * Traverse over all the edges in the graph.
	 * 
	 * @return The traversal representing all the edges in the graph.
	 */
	EdgeTraversal<?, ?, M> e();

	/**
	 * Completely drain the pipeline of its objects. Useful when a sideEffect of the pipeline is desired.
	 */
	void iterate();

	/**
	 * The serves are an arbitrary filter where the filter criteria is provided by the filterFunction.
	 *
	 * @param filterFunction
	 *            the filter function of the pipe
	 * @return the extended Pipeline
	 */
	Traversal<T, ?, ?, M> filter(TraversalFunction<T, Boolean> filterFunction);

	/**
	 * Will emit the object only if it is in the provided collection.
	 *
	 * @param collection
	 *            the collection to retain
	 * @return the extended Pipeline
	 */
	Traversal<T, ?, ?, M> retain(Iterable<?> collection);

	/**
	 * The object that was seen at the topmost marked step is emitted. The mark step is removed from the stack.
	 *
	 * @return the extended Pipeline
	 */
	M back();

	/**
	 * Marks the step so that a subsequent call to back() or optional() may return to this point. If the pipeline is a stack then each call to back or optional
	 * will pop all steps back to and including the previous mark. The next mark in the pipeline is thus exposed.
	 * 
	 * @return the extended Pipeline
	 */
	Traversal<T, C, S, ? extends Traversal<T, C, S, M>> mark();

	/**
	 * Emit the respective property of the incoming element.
	 *
	 * @param <N>
	 *            The type of the property value
	 * @param key
	 *            the property key
	 * @return the extended Pipeline
	 */
	<N> Traversal<N, ?, ?, M> property(String key);

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
	<N> Traversal<? extends N, ?, ?, M> property(String key, Class<N> type);

	/**
	 * Return the number of objects iterated through the pipeline.
	 *
	 * @return the number of objects iterated
	 */
	long count();

	/**
	 * Return the next object in the pipeline.
	 *
	 */
	@Override
	T next();

	/**
	 * Return the next object in the pipeline.
	 *
	 * @param defaultValue
	 *            The value to be returned if there is no next object in the pipeline.
	 * @return returns the next object in the pipeline, if there are no more objects then defaultValue is returned.
	 */
	T nextOrDefault(T defaultValue);

	/**
	 * Return a list of all the objects in the pipeline.
	 *
	 * @return a list of all the objects
	 */
	@Deprecated
	List<? extends T> toList();

	/**
	 * Emit the ids of the incoming objects.
	 *
	 * @param <N>
	 *            The type of the id objects.
	 * @return A traversal of the ids.
	 * @since 2.1.0
	 */
	<N> Traversal<N, ?, ?, M> id();

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
	<N> Traversal<? extends N, ?, ?, M> id(Class<N> c);
}
