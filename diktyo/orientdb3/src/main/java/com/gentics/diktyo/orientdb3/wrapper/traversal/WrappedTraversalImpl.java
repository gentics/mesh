package com.gentics.diktyo.orientdb3.wrapper.traversal;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.tinkerpop.gremlin.structure.Element;

import com.gentics.diktyo.orientdb3.wrapper.factory.WrapperFactory;
import com.gentics.diktyo.wrapper.element.WrappedElement;
import com.gentics.diktyo.wrapper.traversal.AbstractWrappedTraversal;

public class WrappedTraversalImpl<T extends Element> extends AbstractWrappedTraversal<T> {

	private Iterator<T> it;

	public WrappedTraversalImpl(Iterator<T> it) {
		this.it = it;
	}

	@Override
	public <R extends WrappedElement<T>> Stream<R> stream(Class<R> clazzOfR) {
		Stream<T> stream = StreamSupport.stream(
			Spliterators.spliteratorUnknownSize(it, Spliterator.ORDERED),
			false);

		Stream<R> framedStream = stream.map(element -> WrapperFactory.frameElement(element, clazzOfR));
		return framedStream;
	}

	@Override
	public <R> R next(Class<R> clazzOfR) {
		if (it.hasNext()) {
			T element = it.next();
			return WrapperFactory.frameElement(element, clazzOfR);
		} else {
			return null;
		}
	}

}
