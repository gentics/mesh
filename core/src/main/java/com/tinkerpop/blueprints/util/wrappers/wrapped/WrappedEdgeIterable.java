package com.tinkerpop.blueprints.util.wrappers.wrapped;

import com.tinkerpop.blueprints.CloseableIterable;
import com.tinkerpop.blueprints.Edge;

import java.util.Iterator;

/**
 * Patched iterable to use the wrapper
 * @author jotschi
 *
 */
class WrappedEdgeIterable implements CloseableIterable<Edge> {

	private final Iterable<Edge> iterable;

	public WrappedEdgeIterable(final Iterable<Edge> iterable) {
		this.iterable = iterable;
	}

	public Iterator<Edge> iterator() {
		Iterator<Edge> it = new Iterator<Edge>() {
			private final Iterator<Edge> itty = iterable.iterator();

			public void remove() {
				throw new UnsupportedOperationException();
			}

			public boolean hasNext() {
				return this.itty.hasNext();
			}

			public Edge next() {
				return new WrappedEdge(this.itty.next());
			}
		};
		return NullCheckIteratorWrapper.wrap2(it);
	}

	public void close() {
		if (this.iterable instanceof CloseableIterable) {
			((CloseableIterable) iterable).close();
		}
	}

	
}