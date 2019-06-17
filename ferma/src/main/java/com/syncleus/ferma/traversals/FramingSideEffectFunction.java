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
import com.syncleus.ferma.traversals.FrameMaker;

/**
 * Frames the argument before delegation.
 *
 * @param <T>
 */
class FramingSideEffectFunction<T, K extends ElementFrame> extends FrameMaker implements SideEffectFunction<T> {

    private final SideEffectFunction<T> delegate;

    public FramingSideEffectFunction(final SideEffectFunction<T> delegate, final FramedGraph graph, final Class<K> kind) {
        super(graph, kind);
        this.delegate = delegate;

    }

    public FramingSideEffectFunction(final SideEffectFunction<T> delegate, final FramedGraph graph) {
        super(graph);
        this.delegate = delegate;

    }

    @Override
    public void execute(T o) {
        o = makeFrame(o);

        delegate.execute(o);
    }

}
