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

import com.syncleus.ferma.traversals.VertexTraversal;
import com.syncleus.ferma.traversals.EdgeTraversal;
import com.google.gson.JsonObject;
import com.tinkerpop.blueprints.Vertex;

public interface VertexFrame extends ElementFrame {    
    @Override
    Vertex getElement();

    /**
     * Add an edge using the supplied frame type.
     *
     * @param <T> The type for the framed edge.
     * @param label
     *            The label for the edge
     * @param inVertex
     *            The vertex to link to.
     * @param initializer
     *            the initializer for the frame which defines its type and may initialize properties
     * @return The new edge.
     */
    <T> T addFramedEdge(String label, VertexFrame inVertex, ClassInitializer<T> initializer);
    
    /**
     * Add an edge using the supplied frame type.
     *
     * @param <T> The type for the framed edge.
     * @param label
     *            The label for the edge
     * @param inVertex
     *            The vertex to link to.
     * @param kind
     *            The kind of frame.
     * @return The new edge.
     */
    <T> T addFramedEdge(String label, VertexFrame inVertex, Class<T> kind);

    /**
     * Add an edge using the supplied frame type.
     *
     * This will bypass the default type resolution and use the untyped resolver
     * instead. This method is useful for speeding up a look up when type resolution
     * isn't required.
     *
     * @param <T> The type for the framed edge.
     * @param label
     *            The label for the edge
     * @param inVertex
     *            The vertex to link to.
     * @param initializer
     *            the initializer for the frame which defines its type and may initialize properties
     * @return The new edge.
     */
    <T> T addFramedEdgeExplicit(String label, VertexFrame inVertex, ClassInitializer<T> initializer);
    
    /**
     * Add an edge using the supplied frame type.
     *
     * This will bypass the default type resolution and use the untyped resolver
     * instead. This method is useful for speeding up a look up when type resolution
     * isn't required.
     *
     * @param <T> The type for the framed edge.
     * @param label
     *            The label for the edge
     * @param inVertex
     *            The vertex to link to.
     * @param kind
     *            The kind of frame.
     * @return The new edge.
     */
    <T> T addFramedEdgeExplicit(String label, VertexFrame inVertex, Class<T> kind);

    /**
     * Add an edge using a frame type of {@link TEdge}.
     *
     * @param label
     *            The label for the edge
     * @param inVertex
     *            The vertex to link to.
     * @return The added edge.
     */
    TEdge addFramedEdge(String label, VertexFrame inVertex);

    /**
     * Add an edge using a frame type of {@link TEdge}.
     *
     * This will bypass the default type resolution and use the untyped resolver
     * instead. This method is useful for speeding up a look up when type resolution
     * isn't required.
     *
     * @param label
     *            The label for the edge
     * @param inVertex
     *            The vertex to link to.
     * @return The added edge.
     */
    TEdge addFramedEdgeExplicit(String label, VertexFrame inVertex);

    VertexTraversal<?, ?, ?> out(final int branchFactor, final String... labels);

    VertexTraversal<?, ?, ?> out(final String... labels);

    VertexTraversal<?, ?, ?> in(final int branchFactor, final String... labels);

    VertexTraversal<?, ?, ?> in(final String... labels);

    VertexTraversal<?, ?, ?> both(final int branchFactor, final String... labels);

    VertexTraversal<?, ?, ?> both(final String... labels);

    EdgeTraversal<?, ?, ?> outE(final int branchFactor, final String... labels);

    EdgeTraversal<?, ?, ?> outE(final String... labels);

    EdgeTraversal<?, ?, ?> inE(final int branchFactor, final String... labels);

    EdgeTraversal<?, ?, ?> inE(final String... labels);

    EdgeTraversal<?, ?, ?> bothE(final int branchFactor, final String... labels);

    EdgeTraversal<?, ?, ?> bothE(final String... labels);

    /**
     * Create edges from the framed vertex to the supplied vertex with the
     * supplied labels
     *
     * @param vertex
     *            The vertex to link to.
     * @param labels
     *            The labels for the edges.
     */
    void linkOut(VertexFrame vertex, String... labels);

    /**
     * Create edges from the supplied vertex to the framed vertex with the
     * supplied labels
     *
     * @param vertex
     *            The vertex to link from.
     * @param labels
     *            The labels for the edges.
     */
    void linkIn(VertexFrame vertex, String... labels);

    /**
     * Create edges from the supplied vertex to the framed vertex and vice versa
     * with the supplied labels
     *
     * @param vertex
     *            The vertex to link to and from.
     * @param labels
     *            The labels for the edges.
     */
    void linkBoth(VertexFrame vertex, String... labels);

    /**
     * Remove all out edges to the supplied vertex with the supplied labels.
     *
     * @param vertex
     *            The vertex to removed the edges to.
     * @param labels
     *            The labels of the edges.
     */
    void unlinkOut(VertexFrame vertex, String... labels);

    /**
     * Remove all in edges to the supplied vertex with the supplied labels.
     *
     * @param vertex
     *            The vertex to removed the edges from.
     * @param labels
     *            The labels of the edges.
     */
    void unlinkIn(VertexFrame vertex, String... labels);

    /**
     * Remove all edges to/from the supplied vertex with the supplied labels.
     *
     * @param vertex
     *            The vertex to removed the edges to/from.
     * @param labels
     *            The labels of the edges.
     */
    void unlinkBoth(VertexFrame vertex, String... labels);

    /**
     * Remove all out edges with the labels and then add a single edge to the
     * supplied vertex.
     *
     * @param vertex
     *            the vertex to link to.
     * @param labels
     *            The labels of the edges.
     */
    void setLinkOut(VertexFrame vertex, String... labels);

    /**
     * Remove all in edges with the labels and then add a single edge from the
     * supplied vertex.
     *
     * @param vertex
     *            the vertex to link from.
     * @param labels
     *            The labels of the edges.
     */
    void setLinkIn(VertexFrame vertex, String... labels);

    /**
     * Remove all edges with the labels and then add a edges from the
     * supplied vertex and to the supplied vertex.
     *
     * @param vertex
     *            the vertex to link from.
     * @param labels
     *            The labels of the edges.
     */
    void setLinkBoth(VertexFrame vertex, String... labels);

    /**
     * Remove all out edges with the labels and then add a single edge to a new
     * vertex.
     *
     * @param <K> The type used to frame the edge.
     * @param initializer
     *            the initializer for the frame which defines its type and may initialize properties
     * @param labels
     *            The labels of the edges.
     * @return The newly created edge.
     */
    <K> K setLinkOut(ClassInitializer<K> initializer, String... labels);
    
    /**
     * Remove all out edges with the labels and then add a single edge to a new
     * vertex.
     *
     * @param <K> The type used to frame the edge.
     * @param kind
     *            The kind of frame.
     * @param labels
     *            The labels of the edges.
     * @return The newly created edge.
     */
    <K> K setLinkOut(Class<K> kind, String... labels);

    /**
     * Remove all out edges with the labels and then add a single edge to a new
     * vertex.
     *
     * This will bypass the default type resolution and use the untyped resolver
     * instead. This method is useful for speeding up a look up when type resolution
     * isn't required.
     *
     * @param <K> The type used to frame the edge.
     * @param initializer
     *            the initializer for the frame which defines its type and may initialize properties
     * @param labels
     *            The labels of the edges.
     * @return The newly created edge.
     */
    <K> K setLinkOutExplicit(ClassInitializer<K> initializer, String... labels);
    
    /**
     * Remove all out edges with the labels and then add a single edge to a new
     * vertex.
     *
     * This will bypass the default type resolution and use the untyped resolver
     * instead. This method is useful for speeding up a look up when type resolution
     * isn't required.
     *
     * @param <K> The type used to frame the edge.
     * @param kind
     *            The kind of frame.
     * @param labels
     *            The labels of the edges.
     * @return The newly created edge.
     */
    <K> K setLinkOutExplicit(Class<K> kind, String... labels);

    /**
     * Remove all in edges with the labels and then add a single edge from a
     * new vertex.
     *
     * @param <K> The type used to frame the edge.
     * @param initializer
     *            the initializer for the frame which defines its type and may initialize properties
     * @param labels
     *            The labels of the edges.
     * @return The newly created edge.
     */
    <K> K setLinkIn(ClassInitializer<K> initializer, String... labels);
    
    /**
     * Remove all in edges with the labels and then add a single edge from a
     * new vertex.
     *
     * @param <K> The type used to frame the edge.
     * @param kind
     *            The kind of frame.
     * @param labels
     *            The labels of the edges.
     * @return The newly created edge.
     */
    <K> K setLinkIn(Class<K> kind, String... labels);

    /**
     * Remove all in edges with the labels and then add a single edge from a
     * new vertex.
     *
     * This will bypass the default type resolution and use the untyped resolver
     * instead. This method is useful for speeding up a look up when type resolution
     * isn't required.
     *
     * @param <K> The type used to frame the edge.
     * @param initializer
     *            the initializer for the frame which defines its type and may initialize properties
     * @param labels
     *            The labels of the edges.
     * @return The newly created edge.
     */
    <K> K setLinkInExplicit(ClassInitializer<K> initializer, String... labels);
    
    /**
     * Remove all in edges with the labels and then add a single edge from a
     * new vertex.
     *
     * This will bypass the default type resolution and use the untyped resolver
     * instead. This method is useful for speeding up a look up when type resolution
     * isn't required.
     *
     * @param <K> The type used to frame the edge.
     * @param kind
     *            The kind of frame.
     * @param labels
     *            The labels of the edges.
     * @return The newly created edge.
     */
    <K> K setLinkInExplicit(Class<K> kind, String... labels);

    /**
     * Remove all edges with the labels and then add edges to/from a new
     * vertex.
     *
     * @param <K> The type used to frame the edge.
     * @param initializer
     *            the initializer for the frame which defines its type and may initialize properties
     * @param labels
     *            The labels of the edges.
     * @return The newly created edge.
     */
    <K> K setLinkBoth(ClassInitializer<K> initializer, String... labels);
    
    /**
     * Remove all edges with the labels and then add edges to/from a new
     * vertex.
     *
     * @param <K> The type used to frame the edge.
     * @param kind
     *            The kind of frame.
     * @param labels
     *            The labels of the edges.
     * @return The newly created edge.
     */
    <K> K setLinkBoth(Class<K> kind, String... labels);

    /**
     * Remove all edges with the labels and then add edges to/from a new
     * vertex.
     *
     * This will bypass the default type resolution and use the untyped resolver
     * instead. This method is useful for speeding up a look up when type resolution
     * isn't required.
     *
     * @param <K> The type used to frame the edge.
     * @param initializer
     *            the initializer for the frame which defines its type and may initialize properties
     * @param labels
     *            The labels of the edges.
     * @return The newly created edge.
     */
    <K> K setLinkBothExplicit(ClassInitializer<K> initializer, String... labels);
    
    /**
     * Remove all edges with the labels and then add edges to/from a new
     * vertex.
     *
     * This will bypass the default type resolution and use the untyped resolver
     * instead. This method is useful for speeding up a look up when type resolution
     * isn't required.
     *
     * @param <K> The type used to frame the edge.
     * @param kind
     *            The kind of frame.
     * @param labels
     *            The labels of the edges.
     * @return The newly created edge.
     */
    <K> K setLinkBothExplicit(Class<K> kind, String... labels);

    /**
     * Shortcut to get frame Traversal of current element
     *
     * @return The traversal for the current element.
     */
    VertexTraversal<?, ?, ?> traversal();

    /**
     * Output the vertex as JSON.
     *
     * @return A JsonObject representing this frame.
     */
    JsonObject toJson();

    /**
     * Reframe this element as a different type of frame.
     *
     * @param <T> The type used to frame the element.
     * @param kind The new kind of frame.
     * @return The new frame
     */
    <T> T reframe(Class<T> kind);

    /**
     * Reframe this element as a different type of frame.
     *
     * This will bypass the default type resolution and use the untyped resolver
     * instead. This method is useful for speeding up a look up when type resolution
     * isn't required.
     *
     * @param <T> The type used to frame the element.
     * @param kind The new kind of frame.
     * @return The new frame
     */
    <T> T reframeExplicit(Class<T> kind);

}
