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

import java.util.Iterator;
import java.util.List;

import com.syncleus.ferma.traversals.TraversalFunction;

import com.tinkerpop.pipes.Pipe;

public class TraversalFunctionPipe implements TraversalFunction {

    private final TraversalFunction delegate;

    public TraversalFunctionPipe(final TraversalFunction delegate) {
        this.delegate = delegate;
    }

    @Override
    public Object compute(final Object argument) {
        final Object result = delegate.compute(argument);
        if (result instanceof Iterator) {
            final Iterator i = (Iterator) result;
            return new Pipe() {

                @Override
                public boolean hasNext() {
                    return i.hasNext();
                }

                @Override
                public Object next() {
                    return i.next();
                }

                @Override
                public Iterator iterator() {
                    return null;
                }

                @Override
                public void setStarts(final Iterator starts) {
                }

                @Override
                public void setStarts(final Iterable starts) {
                }

                @Override
                public List getCurrentPath() {
                    return null;
                }

                @Override
                public void enablePath(final boolean enable) {
                }

                @Override
                public void reset() {
                }

                @Override
                public void remove() {
                }
            };
        }
        return result;
    }

}
