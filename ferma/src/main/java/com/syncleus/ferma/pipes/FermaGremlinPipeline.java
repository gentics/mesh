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
package com.syncleus.ferma.pipes;

import java.util.List;

import com.tinkerpop.blueprints.Element;
import com.tinkerpop.pipes.PipeFunction;
import com.tinkerpop.pipes.transform.PathPipe;
import com.tinkerpop.pipes.util.FluentUtility;

public class FermaGremlinPipeline<S, E> extends com.tinkerpop.gremlin.java.GremlinPipeline<S, E> {

	private E current;

	public FermaGremlinPipeline() {
		super();
	}

	public FermaGremlinPipeline(final Object starts, final boolean doQueryOptimization) {
		super(starts, doQueryOptimization);
	}

	public FermaGremlinPipeline(final Object starts) {
		super(starts);
	}

	@Override
	public void remove() {
		if (this.current instanceof Element) {
			((Element) this.current).remove();
			this.current = null;
		} else
			throw new UnsupportedOperationException("Current must be an element to remove");
	}

	@Override
	public E next() {

		this.current = super.next();
		return this.current;
	}

	@Override
	public List<E> next(final int number) {
		this.current = null;
		return super.next(number);
	}

	public void removeAll() {
		super.remove();
	}
}
