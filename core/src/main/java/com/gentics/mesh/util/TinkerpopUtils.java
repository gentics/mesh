package com.gentics.mesh.util;

import java.util.Iterator;

import com.tinkerpop.blueprints.CloseableIterable;

public final class TinkerpopUtils {

	public static int count(final Iterator<?> iterator) {
		int counter = 0;
		while (iterator.hasNext()) {
			iterator.next();
			counter++;
		}
		return counter;
	}

	public static int count(final Iterable iterable) {
		return count(iterable.iterator());
	}

	public static int count(final CloseableIterable iterable) {
		return count(iterable.iterator());
	}
}
