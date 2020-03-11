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

import com.syncleus.ferma.ElementFrame;
import com.syncleus.ferma.FramedGraph;
import com.syncleus.ferma.TEdge;
import com.syncleus.ferma.TVertex;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.pipes.util.structures.Pair;

@Deprecated
class FrameMaker {

	private final FramedGraph graph;
	private final Class<?> kind;

	public FrameMaker(final FramedGraph graph, final Class<?> kind) {
		this.graph = graph;
		this.kind = kind;
	}

	public FrameMaker(final FramedGraph graph) {
		this(graph, null);
	}

	<N> N makeFrame(Object o) {
		if (o instanceof Pair) {
			final Pair pair = (Pair) o;
			o = new Pair(makeFrame(pair.getA()), makeFrame(pair.getB()));
		}
		if (kind == null) {
			if (o instanceof Edge)
				o = graph.frameElement((Element) o, TEdge.class);
			else if (o instanceof Vertex)
				o = graph.frameElement((Element) o, TVertex.class);
		} else if (o instanceof Element)
			o = graph.frameElement((Element) o, (Class<ElementFrame>) kind);
		return (N) o;
	}

	<N> N makeFrameExplicit(Object o) {
		if (o instanceof Pair) {
			final Pair pair = (Pair) o;
			o = new Pair(makeFrameExplicit(pair.getA()), makeFrameExplicit(pair.getB()));
		}
		if (kind == null) {
			if (o instanceof Edge)
				o = graph.frameElementExplicit((Element) o, TEdge.class);
			else if (o instanceof Vertex)
				o = graph.frameElementExplicit((Element) o, TVertex.class);
		} else if (o instanceof Element)
			o = graph.frameElementExplicit((Element) o, (Class<ElementFrame>) kind);
		return (N) o;
	}

	protected Object removeFrame(final Object object) {
		if (object instanceof ElementFrame)
			return ((ElementFrame) object).getElement();
		return object;
	}

}
