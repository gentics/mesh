package com.gentics.mesh.graphdb;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

import com.gentics.mesh.core.rest.common.ContainerType;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientBaseGraph;
import com.tinkerpop.blueprints.impls.orient.OrientElementIterable;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;

/**
 * Vertex fetch query variant, that supports filtering and ordering.
 * 
 * @author plyhun
 *
 */
public class MeshOrientGraphVertexQuery extends MeshOrientGraphQuery<Vertex, Optional<ContainerType>> {

	public MeshOrientGraphVertexQuery(Graph iGraph, Class<?> vertexClass) {
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
		text.append(OrientBaseGraph.encodeClassName(vertexClass.getSimpleName()));

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

		if (!((OrientBaseGraph) graph).isUseClassForVertexLabel())
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

		// Explicit fetch plan is not supported by a newer SQL API, so we use it
		// to tell apart the usage of a new and old API.
		if (fetchPlan != null) {
			final OSQLSynchQuery<OIdentifiable> query = new OSQLSynchQuery<OIdentifiable>(sqlQuery);

			query.setFetchPlan(fetchPlan);

			return new OrientElementIterable<Vertex>(((OrientBaseGraph) graph),
					((OrientBaseGraph) graph).getRawGraph().query(query, queryParams.toArray()));
		} else {
			return () -> StreamSupport.stream(((OrientBaseGraph) graph).getRawGraph().query(sqlQuery, queryParams.toArray()), false)
				.map(oresult -> (Vertex) new OrientVertex((OrientBaseGraph) graph, oresult.toElement()))
				.iterator();
		}
	}
}
