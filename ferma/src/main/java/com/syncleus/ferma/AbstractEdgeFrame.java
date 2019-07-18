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
import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.util.wrappers.wrapped.WrappedEdge;
import com.tinkerpop.blueprints.util.wrappers.wrapped.WrappedElement;

/**
 * The base class that all edge frames must extend.
 */
public abstract class AbstractEdgeFrame extends AbstractElementFrame implements EdgeFrame {

	@Override
	public Edge getElement() {
		FramedGraph fg = getGraph();
		if (fg == null) {
			throw new RuntimeException("Could not find framed graph");
		}

		Edge edgeForId = fg.getEdge(id);
		if (edgeForId == null) {
			throw new RuntimeException("No edge for Id {" + id + "} of type {" + getClass().getName() + "} could be found within the graph");
		}
		Element edge = ((WrappedEdge) edgeForId).getBaseElement();

		// Unwrap wrapped vertex
		if (edge instanceof WrappedElement) {
			edge = (Edge) ((WrappedElement) edge).getBaseElement();
		}
		return (Edge) edge;
	}

	@Override
	public String getLabel() {
		return getElement().getLabel();
	}

	@Override
	public VertexTraversal<?, ?, ?> inV() {
		return new SimpleTraversal(getGraph(), this).castToEdges().inV();
	}

	@Override
	public VertexTraversal<?, ?, ?> outV() {
		return new SimpleTraversal(getGraph(), this).castToEdges().outV();
	}

	@Override
	public EdgeTraversal<?, ?, ?> traversal() {
		return new SimpleTraversal(getGraph(), this).castToEdges();
	}

	@Override
	public JsonObject toJson() {
		final JsonObject json = new JsonObject();
		if (getId() instanceof Number)
			json.addProperty("id", getId(Number.class));
		if (getId() instanceof String)
			json.addProperty("id", getId(String.class));
		json.addProperty("elementClass", "edge");
		json.addProperty("label", getLabel());
		for (final String key : getPropertyKeys()) {

			final Object value = getProperty(key);
			if (value instanceof Number)
				json.addProperty(key, (Number) value);
			else if (value instanceof String)
				json.addProperty(key, (String) value);
		}
		json.add("outV", outV().next().toJson());
		json.add("inV", inV().next().toJson());
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
		return getGraph().frameElementExplicitById(getId(), kind);
	}
}
