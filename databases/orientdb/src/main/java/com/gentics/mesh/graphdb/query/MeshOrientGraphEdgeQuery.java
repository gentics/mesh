package com.gentics.mesh.graphdb.query;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.tinkerpop.gremlin.orientdb.OGraph;
import org.apache.tinkerpop.gremlin.orientdb.OrientEdge;
import org.apache.tinkerpop.gremlin.orientdb.OrientGraph;
import org.apache.tinkerpop.gremlin.structure.Edge;

import com.gentics.mesh.core.db.query.MeshGraphEdgeQuery;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.graphdb.AbstractMeshMadlGraphQuery;

/**
 * Edge fetch query variant, that supports filtering and ordering.
 * 
 * @author plyhun
 *
 */
public class MeshOrientGraphEdgeQuery extends AbstractMeshMadlGraphQuery<Edge, Optional<? extends Collection<? extends Class<?>>>, OrientGraph> implements MeshGraphEdgeQuery {

	protected final String edgeLabel;

	public MeshOrientGraphEdgeQuery(OrientGraph iGraph, Class<?> vertexClass, String edgeLabel) {
		super(iGraph, vertexClass);
		this.edgeLabel = edgeLabel;
	}

	/**
	 * Retrieve the ordered edges
	 * 
	 * @param propsAndDirs sorting parameters, in a form of 'field sortOrder', 'field sortOrder', etc.
	 * @return
	 */
	@Override
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
		text.append(encodeClassName(edgeLabel));
		text.append(" ");

		// Build the query params
		final List<Object> queryParams = manageFilters(text);
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
			text.append("ferma_type");
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
		if (maybeCustomFilter.isPresent() && skip > 0 && skip < Integer.MAX_VALUE) {
			text.append(SKIP);
			text.append(skip);
		}

		if (maybeCustomFilter.isPresent() && limit > 0 && limit < Integer.MAX_VALUE) {
			text.append(LIMIT);
			text.append(limit);
		}
		String sqlQuery = text.toString().replace("[edgeType='" + ContainerType.INITIAL.getCode() + "']", "[edgeType='" + ContainerType.PUBLISHED.getCode() + "']");
		log.debug("EDGE QUERY: {}", sqlQuery);

		return () -> StreamSupport.stream(((OGraph) graph).querySql(sqlQuery, IntStream.range(0, queryParams.size()).mapToObj(i -> Pair.of(i, queryParams.get(i))).collect(Collectors.toMap(Pair::getKey, Pair::getValue))).spliterator(), false)
				.map(oresult -> (Edge) new OrientEdge((OGraph) graph, oresult.getRawResult().toElement()))
				.iterator();
	}
}
