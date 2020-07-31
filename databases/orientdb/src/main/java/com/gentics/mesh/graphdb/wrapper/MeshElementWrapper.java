package com.gentics.mesh.graphdb.wrapper;

import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphdb.model.MeshElement;
import com.gentics.mesh.graphdb.spi.Database;
import com.tinkerpop.blueprints.Element;

import io.vertx.core.Vertx;

public abstract class MeshElementWrapper<T extends MeshElement> {
	protected final T delegate;

	protected MeshElementWrapper(T delegate) {
		this.delegate = delegate;
	}

	@Override
	public int hashCode() {
		return delegate.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return delegate.equals(obj);
	}

	@Override
	public String toString() {
		return delegate.toString();
	}

	public void setUuid(String uuid) {
		delegate.setUuid(uuid);
	}

	public String getUuid() {
		return delegate.getUuid();
	}

	public Element getElement() {
		return delegate.getElement();
	}

	public String getElementVersion() {
		return delegate.getElementVersion();
	}

	public <T> T property(String name) {
		return delegate.property(name);
	}

	public void addToStringSetProperty(String propertyKey, String value) {
		delegate.addToStringSetProperty(propertyKey, value);
	}

	public <R> void property(String key, R value) {
		delegate.property(key, value);
	}

	public void removeProperty(String key) {
		delegate.removeProperty(key);
	}

	public Database db() {
		return delegate.db();
	}

	public Vertx vertx() {
		return delegate.vertx();
	}

	public MeshOptions options() {
		return delegate.options();
	}

	public T getDelegate() {
		return delegate;
	}
}
