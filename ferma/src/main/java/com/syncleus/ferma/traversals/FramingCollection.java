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

import com.syncleus.ferma.FramedGraph;
import com.syncleus.ferma.traversals.FrameMaker;
import java.util.Collection;
import java.util.Iterator;

/**
 * Frames elements as they are inserted in to the delegate.
 *
 * @param <E> The type of values to store in the collection.
 * @param <K> The type to frame the values as.
 */
class FramingCollection<E, K> extends FrameMaker implements Collection<E> {

    private final Collection<? super E> delegate;
    private final boolean explicit;

    public FramingCollection(final Collection<? super E> delegate, final FramedGraph graph, final Class<K> kind) {
        super(graph, kind);
        this.delegate = delegate;
        this.explicit = false;
    }

    public FramingCollection(final Collection<? super E> delegate, final FramedGraph graph) {
        super(graph);
        this.delegate = delegate;
        this.explicit = false;
    }

    public FramingCollection(final Collection<? super E> delegate, final FramedGraph graph, final Class<K> kind, final boolean explicit) {
        super(graph, kind);
        this.delegate = delegate;
        this.explicit = explicit;
    }

    public FramingCollection(final Collection<? super E> delegate, final FramedGraph graph, final boolean explicit) {
        super(graph);
        this.delegate = delegate;
        this.explicit = explicit;
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
    public boolean contains(final Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<E> iterator() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object[] toArray() {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T[] toArray(final T[] ts) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean add(E e) {
        e = (this.explicit ? this.<E>makeFrameExplicit(e) : this.<E>makeFrame(e));

        return delegate.add(e);
    }

    @Override
    public boolean remove(final Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsAll(final Collection<?> collection) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(final Collection<? extends E> collection) {
        boolean modified = false;
        for (final E e : collection)
            modified |= add(e);
        return modified;
    }

    @Override
    public boolean removeAll(final Collection<?> collection) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(final Collection<?> collection) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean equals(final Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int hashCode() {
        throw new UnsupportedOperationException();
    }

    public Collection<? super E> getDelegate() {

        return delegate;
    }

}
