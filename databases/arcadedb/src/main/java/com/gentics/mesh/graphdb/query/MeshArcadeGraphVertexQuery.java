package com.gentics.mesh.graphdb.query;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.tinkerpop.gremlin.structure.Vertex;

import com.arcadedb.gremlin.ArcadeGraph;
import com.arcadedb.gremlin.ArcadeVertex;
import com.gentics.mesh.core.db.query.MeshGraphVertexQuery;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.graphdb.AbstractMeshMadlGraphQuery;

/**
 * Vertex fetch query variant, that supports filtering and ordering.
 * 
 * @author plyhun
 *
 */
public class MeshArcadeGraphVertexQuery extends AbstractMeshMadlGraphQuery<Vertex, Optional<ContainerType>, ArcadeGraph> implements MeshGraphVertexQuery {

	public MeshArcadeGraphVertexQuery(ArcadeGraph iGraph, Class<?> vertexClass) {
		super(iGraph, vertexClass);
	}

	public Iterable<Vertex> fetch(Optional<ContainerType> maybeContainerType) {
		if (limit == 0)
			return Collections.emptyList();

		final StringBuilder text = new StringBuilder(512);

		// First build the fields to retrieve and sort by
		text.append(QUERY_SELECT);
		text.append("*");
		// Explicit fetch plan is not supported by a newer SQL API, so we use it
		// to tell apart the usage of a new and old API. On the other hand,
		// the old SQL API does not support custom edge filtering.
		buildOrderFieldRequest(text, false, fetchPlan == null);		
		text.append(QUERY_FROM);
		text.append(encodeClassName(vertexClass.getSimpleName()));

		// Build the query params, including the labels (including the one provided with vertexClass).
		final List<Object> queryParams = manageFilters(text);

		maybeCustomFilter.ifPresent(filter -> {
			if (text.indexOf(QUERY_WHERE) > 0) {
				text.append(QUERY_FILTER_AND);
			} else {
				text.append(QUERY_WHERE);
			}
			text.append(filter);
		});

		manageLabels(queryParams.size() > 0, text);

		// Build the order clause
		if (orderPropsAndDirs != null && orderPropsAndDirs.length > 0) {
			text.append(ORDERBY);
			// format: path.name direction, e.g. 'fields.fullname desc'
			for (String propAndDir : orderPropsAndDirs) {
				if (!propAndDir.equals(orderPropsAndDirs[0])) {
					text.append(", ");
				}
				String[] sortParts = propAndDir.split(" ");
				String sanitizedPart = sanitizeInput(sortParts[0]);
				text.append("`");
				text.append(sanitizedPart.replace(".", "-"));
				text.append("`");
				if (sortParts.length > 1) {
					text.append(" ");
					text.append(sortParts[1]);
				}
			}
		}
		if (maybeCustomFilter.isPresent() && skip > 0 && skip < Integer.MAX_VALUE) {
			text.append(SKIP);
			text.append(skip);
		}
		if (maybeCustomFilter.isPresent() && limit > 0 && limit < Integer.MAX_VALUE) {
			text.append(LIMIT);
			text.append(limit);
		}

		String sqlQuery = maybeContainerType.map(ctype -> text.toString().replace("[edgeType='" + ContainerType.INITIAL.getCode() + "']", "[edgeType='" + ctype.getCode() + "']")).orElseGet(() -> text.toString());

		log.debug("VERTEX QUERY: {}", sqlQuery);

		return () -> graph.sql(sqlQuery).setParameters(queryParams.toArray()).execute().elementStream()
				.map(oresult -> (Vertex) new MeshArcadeVertex(graph, oresult.asVertex()))
				.iterator();
	}

	/**
	 * Wrapper around {@link ArcadeVertex} that lacks public constructor.
	 */
	private class MeshArcadeVertex extends ArcadeVertex {
		protected MeshArcadeVertex(ArcadeGraph graph, com.arcadedb.graph.Vertex baseElement, Object... keyValues) {
			super(graph, baseElement, keyValues);
		}
	}
}
