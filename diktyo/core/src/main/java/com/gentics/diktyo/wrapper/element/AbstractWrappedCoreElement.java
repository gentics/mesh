package com.gentics.diktyo.wrapper.element;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.tinkerpop.gremlin.structure.Element;
import org.apache.tinkerpop.gremlin.structure.Property;

import com.gentics.diktyo.index.IndexManager;

public abstract class AbstractWrappedCoreElement<E extends Element> implements WrappedElement<E> {

	private E delegate;

	@Override
	public E delegate() {
		return delegate;
	}

	protected void setDelegate(E delegate) {
		this.delegate = delegate;
	}

	@Override
	public Object id() {
		return delegate().id();
	}

	@Override
	public void remove() {
		delegate().remove();
	}

	@Override
	public <T> T property(String key) {
		Property<T> p = delegate().property(key);
		return p.value();
	}

	@Override
	public <R> void property(final String key, final R value) {
		delegate().property(key, value);
	}

	@Override
	public void removeProperty(String key) {
		delegate().property(key).remove();
	}

	@Override
	public Set<String> properties() {
		Set<String> set = new HashSet<>();
		Iterator<? extends Property<?>> it = delegate().properties();
		while (it.hasNext()) {
			Property<?> p = it.next();
			set.add(p.key());
		}
		return set;
	}

	@Override
	public IndexManager index() {
		// TODO Auto-generated method stub
		return null;
	}

}
