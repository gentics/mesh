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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

import com.syncleus.ferma.ElementFrame;
import com.syncleus.ferma.FramedGraph;
import com.syncleus.ferma.pipes.FermaGremlinPipeline;
import com.tinkerpop.blueprints.Graph;

/**
 * A simple element traversal.
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
public class SimpleTraversal<T, C, S, M> extends AbstractTraversal<T, C, S, M> {
	private final Deque<MarkId> marks = new ArrayDeque<>();
	private int markId = 0;

	public SimpleTraversal(final FramedGraph graph, final Graph delegate) {
		this(graph, new FermaGremlinPipeline<>(delegate, false));
	}

	public SimpleTraversal(final FramedGraph graph, final Iterator starts) {
		super(graph, new FermaGremlinPipeline<>(starts, false));
	}

	public SimpleTraversal(final FramedGraph graph, final ElementFrame starts) {
		this(graph, new FermaGremlinPipeline<>(starts.getElement(), false));
	}

	public MarkId pushMark(final Traversal<?, ?, ?, ?> traversal) {
		final MarkId mark = new MarkId();
		mark.id = "traversalMark" + markId++;
		mark.traversal = traversal;
		marks.push(mark);

		return mark;
	}

	@Override
	public <W, X, Y, Z> MarkId<W, X, Y, Z> pushMark() {

		return pushMark(this);
	}

	@Override
	public <W, X, Y, Z> MarkId<W, X, Y, Z> popMark() {
		return marks.pop();
	}

	/**
	 * @return Cast the traversal to a {@link VertexTraversal}
	 */
	@Override
	public VertexTraversal<C, S, M> castToVertices() {
		return vertexTraversal;
	}

	/**
	 * @return Cast the traversal to a {@link EdgeTraversal}
	 */
	@Override
	public EdgeTraversal<C, S, M> castToEdges() {
		return edgeTraversal;
	}

	@Override
	protected <W, X, Y, Z> Traversal<W, X, Y, Z> castToTraversal() {
		return (Traversal<W, X, Y, Z>) this;
	}

	@Override
	protected <N> SplitTraversal<N> castToSplit() {
		return splitTraversal;
	}

	private final SplitTraversal splitTraversal = new SplitTraversal() {

		@Override
		public Traversal exhaustMerge() {
			getPipeline().exhaustMerge();
			return castToTraversal();
		}

		@Override
		public Traversal fairMerge() {
			getPipeline().fairMerge();
			return castToTraversal();
		}
	};

	private final EdgeTraversal edgeTraversal = new AbstractEdgeTraversal(getGraph(), getPipeline()) {

		@Override
		public VertexTraversal castToVertices() {
			return vertexTraversal;
		}

		@Override
		public EdgeTraversal castToEdges() {
			return edgeTraversal;
		}

		@Override
		protected Traversal castToTraversal() {
			return SimpleTraversal.this;
		}

		@Override
		public AbstractTraversal.MarkId pushMark() {
			return SimpleTraversal.this.pushMark(this);
		}

		@Override
		public AbstractTraversal.MarkId popMark() {
			return SimpleTraversal.this.popMark();
		}

		@Override
		public SplitTraversal castToSplit() {
			return splitTraversal;
		}

	};

	private final VertexTraversal vertexTraversal = new AbstractVertexTraversal(getGraph(), getPipeline()) {
		@Override
		public VertexTraversal castToVertices() {
			return vertexTraversal;
		}

		@Override
		public EdgeTraversal castToEdges() {
			return edgeTraversal;
		}

		@Override
		protected Traversal castToTraversal() {
			return SimpleTraversal.this;
		}

		@Override
		public AbstractTraversal.MarkId pushMark() {
			return SimpleTraversal.this.pushMark(this);
		}

		@Override
		public AbstractTraversal.MarkId popMark() {
			return SimpleTraversal.this.popMark();
		}

		@Override
		public SplitTraversal castToSplit() {
			return splitTraversal;
		}

	};

}
