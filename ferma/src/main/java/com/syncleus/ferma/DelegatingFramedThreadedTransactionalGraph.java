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

import com.syncleus.ferma.framefactories.FrameFactory;
import com.syncleus.ferma.typeresolvers.TypeResolver;
import com.tinkerpop.blueprints.ThreadedTransactionalGraph;
import com.tinkerpop.blueprints.TransactionalGraph;

import java.util.Collection;

public class DelegatingFramedThreadedTransactionalGraph<G extends ThreadedTransactionalGraph> extends DelegatingFramedTransactionalGraph<G> implements WrapperFramedThreadedTransactionalGraph<G> {
  public DelegatingFramedThreadedTransactionalGraph(final G delegate, final FrameFactory builder, final TypeResolver defaultResolver) {
    super(delegate, builder, defaultResolver);
  }

  public DelegatingFramedThreadedTransactionalGraph(final G delegate) {
    super(delegate);
  }

  public DelegatingFramedThreadedTransactionalGraph(final G delegate, final TypeResolver defaultResolver) {
    super(delegate, defaultResolver);
  }

  public DelegatingFramedThreadedTransactionalGraph(final G delegate, final boolean typeResolution, final boolean annotationsSupported) {
    super(delegate, typeResolution, annotationsSupported);
  }

  public DelegatingFramedThreadedTransactionalGraph(final G delegate, final ReflectionCache reflections, final boolean typeResolution, final boolean annotationsSupported) {
    super(delegate, reflections, typeResolution, annotationsSupported);
  }

  public DelegatingFramedThreadedTransactionalGraph(final G delegate, final Collection<? extends Class<?>> types) {
    super(delegate, types);
  }

  public DelegatingFramedThreadedTransactionalGraph(final G delegate, final boolean typeResolution, final Collection<? extends Class<?>> types) {
    super(delegate, typeResolution, types);
  }

  @Override
  public TransactionalGraph newTransaction() {
    return this.getBaseGraph().newTransaction();
  }
}
