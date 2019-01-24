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
import java.util.Collection;
import java.util.Map;
import java.util.Set;

class FramingMap<T extends ElementFrame> extends FrameMaker implements Map {

    public FramingMap(final Map delegate, final FramedGraph graph) {
        super(graph);
        this.delegate = delegate;
    }

    private final Map delegate;

    public Map getDelegate() {
        return delegate;
    }

    @Override
    public int size() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isEmpty() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsKey(final Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsValue(final Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object get(final Object o) {
        return removeFrame(delegate.get(makeFrame(o)));
    }

    @Override
    public Object put(final Object k, final Object v) {
        return delegate.put(makeFrame(k), makeFrame(v));
    }

    @Override
    public Object remove(final Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void putAll(final Map map) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set keySet() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection values() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set entrySet() {
        return delegate.entrySet();
    }

    @Override
    public String toString() {

        return delegate.toString();
    }

}
