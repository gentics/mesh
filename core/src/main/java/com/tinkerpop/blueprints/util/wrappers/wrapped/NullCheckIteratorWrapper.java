package com.tinkerpop.blueprints.util.wrappers.wrapped;

import java.util.Iterator;
import java.util.NoSuchElementException;

import com.gentics.mesh.madl.frame.ElementFrame;
import com.tinkerpop.blueprints.Element;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class NullCheckIteratorWrapper<T> implements Iterator<T> {

	private static final Logger log = LoggerFactory.getLogger(NullCheckIteratorWrapper.class);

	public static <E> NullCheckIteratorWrapper<E> wrap2(Iterator<E> it) {
		return new NullCheckIteratorWrapper<>(it);
	}

	private Iterator<T> delegate;

	private T validNext;

	public NullCheckIteratorWrapper(Iterator<T> delegate) {
		this.delegate = delegate;
	}

	@Override
	public boolean hasNext() {
		if (validNext != null) {
			return true;
		}
		while (delegate.hasNext()) {
			T next = delegate.next();
			if (isInvalid(next)) {
				continue;
			} else {
				validNext = next;
				return true;
			}
		}
		return false;
	}

	@Override
	public T next() {
		if (validNext == null) {
			hasNext();
		}
		if (validNext == null) {
			throw new NoSuchElementException();
		} else {
			T next = validNext;
			validNext = null;
			return next;

		}
	}

	private <R> boolean isInvalid(R e) {
		return isInvalidWrap(e) || isInvalidFrame(e);
	}

	private <R> boolean isInvalidWrap(R e) {
		if (e instanceof WrappedElement) {
			WrappedElement wrap = ((WrappedElement) e);
			if (wrap.getBaseElement() == null) {
				log.debug("Skipping invalid wrapped element result!");
				return true;
			}
		}
		return false;
	}

	private <R> boolean isInvalidFrame(R e) {
		if (e instanceof ElementFrame) {
			ElementFrame frame = ((ElementFrame) e);
			Element framedElement = frame.getElement();
			if (isInvalidWrap(framedElement)) {
				log.debug("Skipping invalid framed element!");
				return true;
			}
		}
		return false;
	}
}
