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
package com.syncleus.ferma;

import com.gentics.madl.tx.Tx;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.util.wrappers.wrapped.WrappedEdge;
import com.tinkerpop.blueprints.util.wrappers.wrapped.WrappedElement;
import com.tinkerpop.blueprints.util.wrappers.wrapped.WrappedVertex;

/**
 * A framed edge for use when you don't want to create a new frame class. Typically used in traversals.
 *
 */
public final class TEdge extends AbstractEdgeFrame {
	public static final ClassInitializer<TEdge> DEFAULT_INITIALIZER = new DefaultClassInitializer(TEdge.class);

	@Override
	public Edge getElement() {
		FramedGraph fg = Tx.get().getGraph();
		if (fg == null) {
			throw new RuntimeException(
				"Could not find thread local graph. The code is most likely not being executed in the scope of a transaction.");
		}

		Edge edgeForId = fg.getEdge(id);
		if (edgeForId == null) {
			throw new RuntimeException("No edge for Id {" + id + "} of type {" + getClass().getName() + "} could be found within the graph");
		}
		Element edge = ((WrappedEdge) edgeForId).getBaseElement();

		// Unwrap wrapped vertex
		if (edge instanceof WrappedElement) {
			edge = (Edge) ((WrappedElement) edge).getBaseElement();
		}
		return (Edge) edge;

	}

}
