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
 * Source Project: TinkerPop Frames
 * Source URL: https://github.com/tinkerpop/frames
 * Source License: BSD 3-clause
 * When: November, 25th 2014
 */
package com.syncleus.ferma;

import com.tinkerpop.blueprints.Element;

import java.util.Iterator;

public abstract class FramingIterable<T, E extends Element> implements Iterable<T> {

	private final Class<T> kind;
	private final Iterable<E> iterable;
	private final FramedGraph framedGraph;
	private final boolean explicit;

	public FramingIterable(final FramedGraph framedGraph, final Iterable<E> iterable, final Class<T> kind) {
		this.framedGraph = framedGraph;
		this.iterable = iterable;
		this.kind = kind;
		this.explicit = false;
	}

	public FramingIterable(final FramedGraph framedGraph, final Iterable<E> iterable, final Class<T> kind, final boolean explicit) {
		this.framedGraph = framedGraph;
		this.iterable = iterable;
		this.kind = kind;
		this.explicit = explicit;
	}

	@Override
	public Iterator<T> iterator() {
		return new Iterator<T>() {
			private final Iterator<E> iterator = iterable.iterator();

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}

			@Override
			public boolean hasNext() {
				return this.iterator.hasNext();
			}

			@Override
			public T next() {
				if (explicit)
					return framedGraph.frameElementExplicit(this.iterator.next(), kind);
				else
					return framedGraph.frameElement(this.iterator.next(), kind);
			}
		};
	}
}
