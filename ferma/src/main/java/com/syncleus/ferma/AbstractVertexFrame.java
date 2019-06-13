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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.syncleus.ferma.traversals.EdgeTraversal;
import com.syncleus.ferma.traversals.SimpleTraversal;
import com.syncleus.ferma.traversals.VertexTraversal;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.util.wrappers.wrapped.WrappedElement;

/**
 * The base class that all vertex frames must extend.
 */
public abstract class AbstractVertexFrame extends AbstractElementFrame implements VertexFrame {

    @Override
    public Vertex getElement() {
        if(super.getElement() instanceof WrappedElement) {
            return (Vertex) ((WrappedElement)super.getElement()).getBaseElement();
        } else {
            return (Vertex) super.getElement();
        }
    }

    @Override
    public <T> T addFramedEdge(final String label, final VertexFrame inVertex, final ClassInitializer<T> initializer) {

        final Edge edge = getElement().addEdge(label, inVertex.getElement());
        final T framedEdge = getGraph().frameNewElement(edge, initializer);
        return framedEdge;
    }
    
    @Override
    public <T> T addFramedEdge(final String label, final VertexFrame inVertex, final Class<T> kind) {
        return this.addFramedEdge(label, inVertex, new DefaultClassInitializer<>(kind));
    }

    @Override
    public <T> T addFramedEdgeExplicit(final String label, final VertexFrame inVertex, final ClassInitializer<T> initializer) {

        final Edge edge = getElement().addEdge(label, inVertex.getElement());
        final T framedEdge = getGraph().frameNewElementExplicit(edge, initializer);
        return framedEdge;
    }
    
    @Override
    public <T> T addFramedEdgeExplicit(final String label, final VertexFrame inVertex, final Class<T> kind) {
        return this.addFramedEdgeExplicit(label, inVertex, new DefaultClassInitializer<>(kind));
    }

    @Override
    public TEdge addFramedEdge(final String label, final VertexFrame inVertex) {
        return addFramedEdge(label, inVertex, TEdge.DEFAULT_INITIALIZER);
    }

    @Override
    public TEdge addFramedEdgeExplicit(final String label, final VertexFrame inVertex) {
        return addFramedEdgeExplicit(label, inVertex, TEdge.DEFAULT_INITIALIZER);
    }

    @Override
    public VertexTraversal<?, ?, ?> out(final int branchFactor, final String... labels) {
        return new SimpleTraversal(getGraph(), this).castToVertices().out(branchFactor, labels);
    }

    @Override
    public VertexTraversal<?, ?, ?> out(final String... labels) {
        return new SimpleTraversal(getGraph(), this).castToVertices().out(labels);
    }

//    @Override
//    public VertexTraversal<?, ?, ?> in(final int branchFactor, final String... labels) {
//        return new SimpleTraversal(getGraph(), this).castToVertices().in(branchFactor, labels);
//    }

    @Override
    public VertexTraversal<?, ?, ?> in(final String... labels) {
        return new SimpleTraversal(getGraph(), this).castToVertices().in(labels);
    }

    @Override
    public VertexTraversal<?, ?, ?> both(final int branchFactor, final String... labels) {
        return new SimpleTraversal(getGraph(), this).castToVertices().both(branchFactor, labels);
    }

    @Override
    public VertexTraversal<?, ?, ?> both(final String... labels) {
        return new SimpleTraversal(getGraph(), this).castToVertices().both(labels);
    }

    @Override
    public EdgeTraversal<?, ?, ?> outE(final int branchFactor, final String... labels) {
        return new SimpleTraversal(getGraph(), this).castToVertices().outE(branchFactor, labels);
    }

    @Override
    public EdgeTraversal<?, ?, ?> outE(final String... labels) {
        return new SimpleTraversal(getGraph(), this).castToVertices().outE(labels);
    }

    @Override
    public EdgeTraversal<?, ?, ?> inE(final int branchFactor, final String... labels) {
        return new SimpleTraversal(getGraph(), this).castToVertices().inE(branchFactor, labels);
    }

    @Override
    public EdgeTraversal<?, ?, ?> inE(final String... labels) {
        return new SimpleTraversal(getGraph(), this).castToVertices().inE(labels);
    }

    @Override
    public EdgeTraversal<?, ?, ?> bothE(final int branchFactor, final String... labels) {
        return new SimpleTraversal(getGraph(), this).castToVertices().bothE(branchFactor, labels);
    }

    @Override
    public EdgeTraversal<?, ?, ?> bothE(final String... labels) {
        return new SimpleTraversal(getGraph(), this).castToVertices().bothE(labels);
    }

    @Override
    public void linkOut(final VertexFrame vertex, final String... labels) {
        for (final String label : labels)
            traversal().linkOut(label, vertex).iterate();
    }

    @Override
    public void linkIn(final VertexFrame vertex, final String... labels) {
        for (final String label : labels)
            traversal().linkIn(label, vertex).iterate();
    }

    @Override
    public void linkBoth(final VertexFrame vertex, final String... labels) {
        for (final String label : labels)
            traversal().linkBoth(label, vertex).iterate();
    }

    @Override
    public void unlinkOut(final VertexFrame vertex, final String... labels) {
        if (vertex != null)
            outE(labels).mark().inV().retain(vertex).back().removeAll();
        else
            outE(labels).removeAll();
    }

    @Override
    public void unlinkIn(final VertexFrame vertex, final String... labels) {
        if (vertex != null)
            inE(labels).mark().outV().retain(vertex).back().removeAll();
        else
            inE(labels).removeAll();
    }

    @Override
    public void unlinkBoth(final VertexFrame vertex, final String... labels) {
        if (vertex != null)
            bothE(labels).mark().bothV().retain(vertex).back().removeAll();
        else
            bothE(labels).removeAll();
    }

    @Override
    public void setLinkOut(final VertexFrame vertex, final String... labels) {
        unlinkOut(null, labels);
        if (vertex != null)
            linkOut(vertex, labels);
    }

    @Override
    public void setLinkIn(final VertexFrame vertex, final String... labels) {
        unlinkIn(null, labels);
        if (vertex != null)
            linkIn(vertex, labels);
    }

    @Override
    public void setLinkBoth(final VertexFrame vertex, final String... labels) {
        unlinkBoth(null, labels);
        if (vertex != null)
            linkBoth(vertex, labels);
    }

    @Override
    public <K> K setLinkOut(final ClassInitializer<K> initializer, final String... labels) {
        final K vertex = getGraph().addFramedVertex(null, initializer);
        setLinkOut((VertexFrame) vertex, labels);
        return vertex;
    }
    
    @Override
    public <K> K setLinkOut(final Class<K> kind, final String... labels) {
        return this.setLinkOut(new DefaultClassInitializer<>(kind), labels);
    }

    @Override
    public <K> K setLinkOutExplicit(final ClassInitializer<K> initializer, final String... labels) {
        final K vertex = getGraph().addFramedVertexExplicit(initializer);
        setLinkOut((VertexFrame) vertex, labels);
        return vertex;
    }
    
    @Override
    public <K> K setLinkOutExplicit(final Class<K> kind, final String... labels) {
        return this.setLinkOutExplicit(new DefaultClassInitializer<>(kind), labels);
    }

    @Override
    public <K> K setLinkIn(final ClassInitializer<K> initializer, final String... labels) {
        final K vertex = getGraph().addFramedVertex(null, initializer);
        setLinkIn((VertexFrame) vertex, labels);
        return vertex;
    }
    
    @Override
    public <K> K setLinkIn(final Class<K> kind, final String... labels) {
        return this.setLinkIn(new DefaultClassInitializer<>(kind), labels);
    }

    @Override
    public <K> K setLinkInExplicit(final ClassInitializer<K> initializer, final String... labels) {
        final K vertex = getGraph().addFramedVertexExplicit(initializer);
        setLinkIn((VertexFrame) vertex, labels);
        return vertex;
    }
    
    @Override
    public <K> K setLinkInExplicit(final Class<K> kind, final String... labels) {
        return this.setLinkInExplicit(new DefaultClassInitializer<>(kind), labels);
    }

    @Override
    public <K> K setLinkBoth(final ClassInitializer<K> initializer, final String... labels) {
        final K vertex = getGraph().addFramedVertex(null, initializer);
        setLinkBoth((VertexFrame) vertex, labels);
        return vertex;
    }
    
    @Override
    public <K> K setLinkBoth(final Class<K> kind, final String... labels) {
        return this.setLinkBoth(new DefaultClassInitializer<>(kind), labels);
    }

    @Override
    public <K> K setLinkBothExplicit(final ClassInitializer<K> initializer, final String... labels) {
        final K vertex = getGraph().addFramedVertexExplicit(initializer);
        setLinkBoth((VertexFrame) vertex, labels);
        return vertex;
    }
    
    @Override
    public <K> K setLinkBothExplicit(final Class<K> kind, final String... labels) {
        return this.setLinkBothExplicit(new DefaultClassInitializer<>(kind), labels);
    }

    @Override
    public VertexTraversal<?, ?, ?> traversal() {
        return new SimpleTraversal(getGraph(), this).castToVertices();
    }

    @Override
    public JsonObject toJson() {
        final JsonObject json = new JsonObject();
        if (getId() instanceof Number)
            json.addProperty("id", getId(Number.class));
        if (getId() instanceof String)
            json.addProperty("id", getId(String.class));
        json.addProperty("elementClass", "vertex");
        for (final String key : getPropertyKeys()) {

            final Object value = getProperty(key);
            if (value instanceof Number)
                json.addProperty(key, (Number) value);
            else if (value instanceof String)
                json.addProperty(key, (String) value);
        }
        return json;
    }

    @Override
    public String toString() {
        final Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(toJson());
    }

    @Override
    public <T> T reframe(final Class<T> kind) {
        return getGraph().frameElement(getElement(), kind);
    }

    @Override
    public <T> T reframeExplicit(final Class<T> kind) {
        return getGraph().frameElementExplicit(getElement(), kind);
    }
}
