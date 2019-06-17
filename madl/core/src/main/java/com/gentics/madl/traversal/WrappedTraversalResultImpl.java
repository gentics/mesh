package com.gentics.madl.traversal;

import java.util.Iterator;

import com.gentics.mesh.madl.frame.ElementFrame;

public class WrappedTraversalResultImpl<T extends ElementFrame> extends AbstractWrappedTraversal<T> {

	private Iterable<T> it;

	public WrappedTraversalResultImpl(Iterable<T> it) {
		this.it = it;
	}

	public WrappedTraversalResultImpl(Iterator<T> it) {
		this.it = () -> it;
	}

	@Override
	public Iterator<T> iterator() {
		return it.iterator();
	}
}
