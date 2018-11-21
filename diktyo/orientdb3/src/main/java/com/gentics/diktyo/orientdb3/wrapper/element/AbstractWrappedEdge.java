package com.gentics.diktyo.orientdb3.wrapper.element;

import org.apache.tinkerpop.gremlin.structure.Vertex;

import com.gentics.diktyo.orientdb3.wrapper.factory.WrapperFactory;
import com.gentics.diktyo.wrapper.element.AbstractWrappedCoreEdge;

public abstract class AbstractWrappedEdge extends AbstractWrappedCoreEdge {

	@Override
	public <R> R inV(Class<R> classOfR) {
		Vertex v = delegate().inVertex();
		return WrapperFactory.frameElement(v, classOfR);
	}

	@Override
	public <R> R outV(Class<R> classOfR) {
		Vertex v = delegate().outVertex();
		return WrapperFactory.frameElement(v, classOfR);
	}
}
