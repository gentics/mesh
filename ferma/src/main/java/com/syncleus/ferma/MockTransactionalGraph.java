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

import com.tinkerpop.blueprints.*;

public class MockTransactionalGraph implements TransactionalGraph {
  private final Graph delegate;

  public MockTransactionalGraph(final Graph delegate) {
    this.delegate = delegate;
  }

  @Override
  public Features getFeatures() {
    return delegate.getFeatures();
  }

  @Override
  public Vertex addVertex(final Object id) {
    return delegate.addVertex(id);
  }

  @Override
  public Vertex getVertex(final Object id) {
    return delegate.getVertex(id);
  }

  @Override
  public void removeVertex(final Vertex vertex) {
    delegate.removeVertex(vertex);
  }

  @Override
  public Iterable<Vertex> getVertices() {
    return delegate.getVertices();
  }

  @Override
  public Iterable<Vertex> getVertices(final String key, final Object value) {
    return delegate.getVertices(key, value);
  }

  @Override
  public Edge addEdge(final Object id, final Vertex outVertex, final Vertex inVertex, final String label) {
    return delegate.addEdge(id, outVertex, inVertex, label);
  }

  @Override
  public Edge getEdge(final Object id) {
    return delegate.getEdge(id);
  }

  @Override
  public void removeEdge(final Edge edge) {
    delegate.removeEdge(edge);
  }

  @Override
  public Iterable<Edge> getEdges() {
    return delegate.getEdges();
  }

  @Override
  public Iterable<Edge> getEdges(final String key, final Object value) {
    return delegate.getEdges(key, value);
  }

  @Override
  public GraphQuery query() {
    return delegate.query();
  }

  @Override
  public void shutdown() {
    delegate.shutdown();
  }

  @Override
  public void stopTransaction(final Conclusion conclusion) {
  }

  @Override
  public void commit() {

  }

  @Override
  public void rollback() {

  }
}
