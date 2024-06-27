package com.gentics.mesh.util;

import java.util.Iterator;

/**
 * Iterate over all wrapped exceptions.
 */
public class ExceptionIterator implements Iterator<Throwable> {

	private Throwable current;

	public ExceptionIterator(Throwable t) {
		this.current = t;
	}

	@Override
	public boolean hasNext() {
		return current != null;
	}

	@Override
	public Throwable next() {
		Throwable ret = current;
		current = current.getCause();
		return ret;
	}
}
