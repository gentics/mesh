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

import java.util.Comparator;

import com.syncleus.ferma.ElementFrame;
import com.syncleus.ferma.FramedGraph;

/**
 * Framed elements before delegation.
 *
 * @param <T>
 */
@Deprecated
class FramingComparator<T, K extends ElementFrame> extends FrameMaker implements Comparator<T> {

	private final Comparator<T> delegate;

	public FramingComparator(final Comparator<T> delegate, final FramedGraph graph) {
		super(graph);
		this.delegate = delegate;

	}

	public FramingComparator(final Comparator<T> delegate, final FramedGraph graph, final Class<K> kind) {
		super(graph, kind);
		this.delegate = delegate;
	}

	@Override
	public int compare(T t, T t1) {

		t = makeFrame(t);
		t1 = makeFrame(t1);

		return delegate.compare(t, t1);
	}

}
