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
package com.gentics.mesh.core.db;

import java.util.function.Consumer;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.apache.tinkerpop.gremlin.structure.Graph;

import com.gentics.madl.graph.DelegatingFramedMadlGraph;
import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.data.HibElement;
import com.gentics.mesh.madl.frame.ElementFrame;
import com.gentics.mesh.util.StreamUtil;

/**
 * An abstract class that can be used to implement vendor specific graph database Tx classes.
 */
public abstract class AbstractTx<G extends Graph, T extends DelegatingFramedMadlGraph<G>> extends com.gentics.madl.tx.AbstractTx<G, T> implements GraphDBTx {

	@Override
	public abstract T getGraph();

	@Override
	public <B extends HibElement> B create(String uuid, Class<? extends B> classOfB, Consumer<B> inflater) {
		B entity = getGraph().addFramedVertex(classOfB);
		if (StringUtils.isNotBlank(uuid)) {
			entity.setUuid(uuid);
		}
		inflater.accept(entity);
		persist(entity);
		return entity;
	}
	
	@Override
	public <B extends HibElement> B persist(B element) {
		/*
		 * Since OrientDB does not tell apart POJOs and persistent entities, 
		 * processing the entity updates directly into the persistent state, 
		 * the merge implementation here is empty.
		 */
		return element;
	}
	
	@Override
	public <B extends HibElement> void delete(B element) {
		((ElementFrame) element).remove();
	}

	@Override
	public <B extends HibElement> long count(Class<? extends B> classOfB) {
		if (HibBaseElement.class.isAssignableFrom(classOfB)) {
			return data().mesh().database().count((Class<? extends HibBaseElement>) classOfB);
		} else {
			return StreamUtil.toStream(getGraph().getFramedVertices("ferma_type", classOfB.getName(), classOfB)).count();
		}
	}

	@Override
	public <B extends HibElement> B load(Object id, Class<? extends B> classOfB) {
		B b = getGraph().getFramedVertexExplicit(classOfB, id);
		try {
			b.getUuid();
		} catch (Throwable e) {
			b = null;
		}
		return b;
	}

	@Override
	public <I extends HibElement, B extends I> Stream<I> loadAll(Class<B> classOfB) {
		return StreamUtil.toStream(getGraph().getFramedVertices(classOfB));
	}
}
