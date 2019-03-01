/**
 * Copyright 2004 - 2017 Syncleus, Inc.
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
/**
 * This product currently only contains code developed by authors
 * of specific components, as identified by the source code files.
 *
 * Since product implements StAX API, it has dependencies to StAX API
 * classes.
 *
 * For additional credits (generally to people who reported problems)
 * see CREDITS file.
 */
package com.syncleus.ferma.ext.orientdb;

import com.syncleus.ferma.ClassInitializer;
import com.syncleus.ferma.DefaultClassInitializer;
import com.syncleus.ferma.DelegatingFramedTransactionalGraph;
import com.syncleus.ferma.VertexFrame;
import com.syncleus.ferma.WrapperFramedTransactionalGraph;
import com.syncleus.ferma.typeresolvers.TypeResolver;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;

public class DelegatingFramedOrientGraph extends DelegatingFramedTransactionalGraph<OrientGraph>
		implements WrapperFramedTransactionalGraph<OrientGraph> {

	public DelegatingFramedOrientGraph(OrientGraph delegate, TypeResolver typeResolver) {
		super(delegate, typeResolver);
	}

	@Override
	public <T> T addFramedVertex(Object id, final ClassInitializer<T> initializer) {
		return frameNewElement(this.getBaseGraph().addVertex(id), initializer);
	}

	@Override
	public <T> T addFramedEdge(Object id, VertexFrame source, VertexFrame destination, String label, ClassInitializer<T> initializer) {
		return frameNewElement(this.getBaseGraph().addEdge(id, source.getElement(), destination.getElement(), label), initializer);
	}

	@Override
	public <T> T addFramedVertex(final Class<T> kind) {
		return this.addFramedVertex("class:" + kind.getSimpleName(), new DefaultClassInitializer<>(kind));
	}

	@Override
	public <T> T addFramedEdge(VertexFrame source, VertexFrame destination, String label, Class<T> kind) {
		return super.addFramedEdge(source, destination, label, kind);
	}

	@Override
	public void stopTransaction(Conclusion conclusion) {
		getBaseGraph().stopTransaction(conclusion);
	}

	@Override
	public void commit() {
		getBaseGraph().commit();
	}

	@Override
	public void rollback() {
		getBaseGraph().rollback();
	}

}
