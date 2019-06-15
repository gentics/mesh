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
package com.syncleus.ferma;

import com.syncleus.ferma.typeresolvers.TypeResolver;
import com.tinkerpop.blueprints.TransactionalGraph;

public class DelegatingFramedTransactionalGraph<G extends TransactionalGraph> extends DelegatingFramedGraph<G>
	implements WrapperFramedTransactionalGraph<G> {

	public DelegatingFramedTransactionalGraph(final G delegate, final TypeResolver defaultResolver) {
		super(delegate, defaultResolver);
	}

	@Override
	public void stopTransaction(final TransactionalGraph.Conclusion conclusion) {
		((TransactionalGraph) this.getBaseGraph()).stopTransaction(conclusion);
	}

	@Override
	public void commit() {
		((TransactionalGraph) this.getBaseGraph()).commit();
	}

	@Override
	public void rollback() {
		((TransactionalGraph) this.getBaseGraph()).rollback();
	}
}
