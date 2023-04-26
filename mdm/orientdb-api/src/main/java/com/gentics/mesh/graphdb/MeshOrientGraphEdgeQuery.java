package com.gentics.mesh.graphdb;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

import com.gentics.mesh.madl.frame.ElementFrame;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.impls.orient.OrientBaseGraph;
import com.tinkerpop.blueprints.impls.orient.OrientEdge;
import com.tinkerpop.blueprints.impls.orient.OrientElementIterable;

/**
 * Edge fetch query variant, that supports filtering and ordering.
 * 
 * @author plyhun
 *
 */
public class MeshOrientGraphEdgeQuery extends MeshOrientGraphQuery<Edge, Optional<? extends Collection<? extends Class<?>>>> {

	protected final String edgeLabel;

	public MeshOrientGraphEdgeQuery(Graph iGraph, Class<?> vertexClass, String edgeLabel) {
		super(iGraph, vertexClass);
		this.edgeLabel = edgeLabel;
	}

	/**
	 * Retrieve the ordered edges
	 * 
	 * @param propsAndDirs sorting parameters, in a form of 'field sortOrder', 'field sortOrder', etc.
	 * @return
	 */
	public Iterable<Edge> fetch(Optional<? extends Collection<? extends Class<?>>> maybeFermaTypes) {
		if (limit == 0)
			return Collections.emptyList();

		final StringBuilder text = new StringBuilder(512);

		// First build the fields to retrieve and sort by
		text.append(QUERY_SELECT);
		text.append("*");		
		// Explicit fetch plan is not supported by a newer SQL API, so we use it
		// to tell apart the usage of a new and old API. On the other hand,
		// the old SQL API does not support custom edge filtering.
		buildOrderFieldRequest(text, true, fetchPlan == null);
		text.append(" ");
		text.append(QUERY_FROM);
		text.append(OrientBaseGraph.encodeClassName(edgeLabel));
		text.append(" ");

		// Build the query params
		final List<Object> queryParams = manageFilters(text);
		if (!((OrientBaseGraph) graph).isUseClassForVertexLabel())
			manageLabels(queryParams.size() > 0, text);
		
		// Build the extra query param for the target vertex name, if specified with the 'vertexClass'
		if (vertexClass != null) {
			if (hasContainers != null && hasContainers.size() > 0) {
				text.append(QUERY_FILTER_AND);				
			} else {
				text.append(QUERY_WHERE);
			}
			maybeFermaTypes.ifPresentOrElse(types -> {
				text.append(" [");
				text.append(" '");
				text.append(vertexClass.getSimpleName());
				text.append("' ");
				types.stream().forEach(ft -> {
					text.append(",'");
					text.append(ft.getSimpleName());
					text.append("'");
				});
				text.append("] CONTAINS ");
			}, () -> {
				text.append(" '");
				text.append(vertexClass.getSimpleName());
				text.append("' = ");
			});
			text.append(relationDirection.name().toLowerCase());
			text.append("V().");
			text.append(ElementFrame.TYPE_RESOLUTION_KEY);			
		}
	
		maybeCustomFilter.ifPresent(filter -> {
			if (vertexClass != null) {
				text.append(QUERY_FILTER_AND);				
			} else {
				text.append(QUERY_WHERE);
			}
			text.append(filter);
		});

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
		if (skip > 0 && skip < Integer.MAX_VALUE) {
			text.append(SKIP);
			text.append(skip);
		}

		if (limit > 0 && limit < Integer.MAX_VALUE) {
			text.append(LIMIT);
			text.append(limit);
		}
		String sqlQuery = text.toString();
		log.debug("EDGE QUERY: {}", sqlQuery);

		// Explicit fetch plan is not supported by a newer SQL API, so we use it
		// to tell apart the usage of a new and old API.
		if (fetchPlan != null) {
			final OSQLSynchQuery<OIdentifiable> query = new OSQLSynchQuery<OIdentifiable>(sqlQuery);
			query.setFetchPlan(fetchPlan);
			return new OrientElementIterable<Edge>(((OrientBaseGraph) graph),
					((OrientBaseGraph) graph).getRawGraph().query(query, queryParams.toArray()));
		} else {
			return () -> StreamSupport.stream(((OrientBaseGraph) graph).getRawGraph().query(sqlQuery, queryParams.toArray()), false)
					.map(oresult -> (Edge) new OrientEdgeImpl((OrientBaseGraph) graph, oresult.toElement()))
					.iterator();
		}
	}
	
	/**
	 * For whatever reason Tinkerpop Blueprints ORM did not provide an edge entity, so it has to be done manually.
	 * 
	 * @author plyhun
	 *
	 */
	private final class OrientEdgeImpl extends OrientEdge {
		public OrientEdgeImpl(final OrientBaseGraph rawGraph, final OIdentifiable rawEdge) {
			super(rawGraph, rawEdge);
		}
	}
}
