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
package com.syncleus.ferma.typeresolvers;

import com.syncleus.ferma.traversals.EdgeTraversal;
import com.syncleus.ferma.traversals.VertexTraversal;
import com.tinkerpop.blueprints.Element;

/**
 * Type resolvers resolve the frame type from the element being requested and may optionally store metadata about the frame type on the element.
 */
public interface TypeResolver {
	/**
	 * Resolve the type of frame that a an element should be.
	 * 
	 * @param <T>
	 *            The type used to frame the element.
	 * @param element
	 *            The element that is being framed.
	 * @param kind
	 *            The kind of frame that is being requested by the client code.
	 * @return The kind of frame
	 */
	<T> Class<? extends T> resolve(Element element, Class<T> kind);

	/**
	 * Resolve the type of frame that a an element should be.
	 * 
	 * @param element
	 *            The element that is being framed.
	 * @return The kind of frame, null if no type resolution properties exist.
	 */
	Class<?> resolve(Element element);

	/**
	 * Called to initialize an element with type resolution properties.
	 * 
	 * @param element
	 *            The element that was created.
	 * @param kind
	 *            The kind of frame that was resolved.
	 */
	void init(Element element, Class<?> kind);

	/**
	 * Called to remove the type resolution properties from an element
	 * 
	 * @param element
	 *            The element to remove the property from.
	 */
	void deinit(Element element);

	/**
	 * Filters the objects on the traversal that satisfy a requested type.
	 * 
	 * @param traverser
	 *            A traversal pointing to the current set of vertex to be filtered
	 * @param type
	 *            The type to filter by.
	 * @return The traversal stream filtered by the desired type.
	 */
	VertexTraversal<?, ?, ?> hasType(VertexTraversal<?, ?, ?> traverser, Class<?> type);

	/**
	 * Filters the objects on the traversal that satisfy a requested type.
	 * 
	 * @param traverser
	 *            A traversal pointing to the current set of edges to be filtered
	 * @param type
	 *            The type to filter by.
	 * @return The traversal stream filtered by the desired type.
	 */
	EdgeTraversal<?, ?, ?> hasType(EdgeTraversal<?, ?, ?> traverser, Class<?> type);

	/**
	 * Filters out the objects on the traversal that are not satisfying a requested type.
	 * 
	 * @param traverser
	 *            A traversal pointing to the current set of vertex to be filtered
	 * @param type
	 *            The type to filter by.
	 * @return The traversal stream filtered by the desired type.
	 */
	VertexTraversal<?, ?, ?> hasNotType(VertexTraversal<?, ?, ?> traverser, Class<?> type);

	/**
	 * Filters out the objects on the traversal that are not satisfying a requested type.
	 * 
	 * @param traverser
	 *            A traversal pointing to the current set of edges to be filtered
	 * @param type
	 *            The type to filter by.
	 * @return The traversal stream filtered by the desired type.
	 */
	EdgeTraversal<?, ?, ?> hasNotType(EdgeTraversal<?, ?, ?> traverser, Class<?> type);
}
