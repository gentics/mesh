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
package com.syncleus.ferma;

import java.util.Set;

import com.gentics.madl.tx.Tx;
import com.syncleus.ferma.traversals.EdgeTraversal;
import com.syncleus.ferma.traversals.VertexTraversal;

/**
 * The base of all framed elements.
 */
public abstract class AbstractElementFrame implements ElementFrame {

	protected Object id;

	/**
	 * This method is called anytime an element is instantiated. If the element is a new element or an existing element this method will be called.
	 *
	 * @param id
	 *            The id of the element
	 */
	protected void init(final Object id) {
		this.id = id;
	}

	/**
	 * This method is only called when creating new elements that don't already exist in the graph. This method should be overridden to initialize any
	 * properties of an element on its creation. If an element is being framed that already exists in the graph this method will not be called.
	 */
	protected void init() {

	}

	@SuppressWarnings("unchecked")
	public Object getId() {
		return id;
	}

	@Override
	public Set<String> getPropertyKeys() {
		return getElement().getPropertyKeys();
	}

	@Override
	public Class<?> getTypeResolution() {
		return getGraph().getTypeResolver().resolve(getElement());
	}

	@Override
	public void setTypeResolution(final Class<?> type) {
		getGraph().getTypeResolver().init(getElement(), type);
	}

	@Override
	public void removeTypeResolution() {
		getGraph().getTypeResolver().deinit(getElement());
	}

	@Override
	public void remove() {
		getElement().remove();
	}

	@Override
	public FramedGraph getGraph() {
		return Tx.get().getGraph();
	}

	@Override
	public <T> T getGraphAttribute(String key) {
		return getGraph().getAttribute(key);
	}

	@Override
	public <T> T getProperty(final String name) {
		return getElement().getProperty(name);
	}

	@Override
	public <T> T getProperty(final String name, final Class<T> type) {
		if (type.isEnum()) {
			return (T) Enum.valueOf((Class<Enum>) type, (String) getElement().getProperty(name));
		}
		return getElement().getProperty(name);
	}

	@Override
	public void setProperty(final String name, final Object value) {
		if (value == null) {
			getElement().removeProperty(name);
		} else if (value instanceof Enum) {
			getElement().setProperty(name, value.toString());
		} else {
			getElement().setProperty(name, value);
		}
	}

	@Override
	public VertexTraversal<?, ?, ?> v() {
		return getGraph().v();
	}

	@Override
	public EdgeTraversal<?, ?, ?> e() {
		return getGraph().e();
	}

	@Override
	public EdgeTraversal<?, ?, ?> e(final Object... ids) {
		return getGraph().e(ids);
	}

	@Override
	public int hashCode() {
		return getElement().hashCode();
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o)
			return true;
		if (o == null)
			return false;
		if (getClass() != o.getClass())
			return false;
		final AbstractElementFrame other = (AbstractElementFrame) o;
		if (getElement() == null) {
			if (other.getElement() != null)
				return false;
		} else if (!getElement().equals(other.getElement()))
			return false;
		return true;
	}

	protected <N> N getId(final Class<N> clazz) {
		return (N) getId();
	}

	@Override
	public String toString() {
		return getElement().toString();
	}

}
