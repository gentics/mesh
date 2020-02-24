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
package com.syncleus.ferma.typeresolvers;

import com.syncleus.ferma.AbstractEdgeFrame;
import com.syncleus.ferma.AbstractVertexFrame;
import com.syncleus.ferma.EdgeFrame;
import com.syncleus.ferma.TEdge;
import com.syncleus.ferma.TVertex;
import com.syncleus.ferma.VertexFrame;
import com.syncleus.ferma.traversals.EdgeTraversal;
import com.syncleus.ferma.traversals.TraversalFunction;
import com.syncleus.ferma.traversals.VertexTraversal;
import com.tinkerpop.blueprints.Element;

/**
 * This type resolver simply returns the type requested by the client.
 */
public class UntypedTypeResolver implements TypeResolver {

	@Override
	public <T> Class<? extends T> resolve(final Element element, final Class<T> kind) {
		if (VertexFrame.class.equals(kind) || AbstractVertexFrame.class.equals(kind))
			return (Class<? extends T>) TVertex.class;
		else if (EdgeFrame.class.equals(kind) || AbstractEdgeFrame.class.equals(kind))
			return (Class<? extends T>) TEdge.class;
		return kind;
	}

	@Override
	public Class<?> resolve(final Element element) {
		return null;
	}

	@Override
	public void init(final Element element, final Class<?> kind) {
	}

	@Override
	public void deinit(final Element element) {
	}

	@Override
	public VertexTraversal<?, ?, ?> hasType(final VertexTraversal<?, ?, ?> traverser, final Class<?> type) {
		return traverser.filter(new TraversalFunction<VertexFrame, Boolean>() {
			@Override
			public Boolean compute(VertexFrame argument) {
				return false;
			}
		});
	}

	@Override
	public EdgeTraversal<?, ?, ?> hasType(final EdgeTraversal<?, ?, ?> traverser, final Class<?> type) {
		return traverser.filter(new TraversalFunction<EdgeFrame, Boolean>() {
			@Override
			public Boolean compute(EdgeFrame argument) {
				return false;
			}
		});
	}

}
