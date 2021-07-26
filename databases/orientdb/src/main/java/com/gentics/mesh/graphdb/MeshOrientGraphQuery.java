package com.gentics.mesh.graphdb;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.Triple;

import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.relationship.GraphRelationships;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientBaseGraph;
import com.tinkerpop.blueprints.impls.orient.OrientElementIterable;
import com.tinkerpop.blueprints.impls.orient.OrientGraphQuery;

public class MeshOrientGraphQuery extends OrientGraphQuery {
	
	protected static final String QUERY_SELECT = "select ";
	protected static final String QUERY_FROM = "from ";
	
	protected final Class<? extends MeshVertex> vertexClass; 
	protected String[] propsAndDirs;

	public MeshOrientGraphQuery(Graph iGraph, Class<? extends MeshVertex> vertexClass) {
		super(iGraph);
		this.vertexClass = vertexClass;
	}

	public MeshOrientGraphQuery hasAll(final String[] key, final Object[] value) {
		for (int i = 0; i < key.length; i++) {
			super.has(key[i], value[i]);
		}
		return this;
	}
	
	public MeshOrientGraphQuery order(final String[] propsAndDirs) {
	    this.propsAndDirs = propsAndDirs;
	    return this;
	  }

	@Override
	public Iterable<Vertex> vertices() {
		if (limit == 0)
			return Collections.emptyList();

		final StringBuilder text = new StringBuilder(512);

		// The default SELECT does not support joining linked vertices,
		// so some extra engineering is required.
		text.append(QUERY_SELECT);
		
		if (this.propsAndDirs != null && this.propsAndDirs.length > 0) {
			text.append("*");
			// format: path.name direction, e.g. 'fields.fullname desc'
			for (String propAndDir : this.propsAndDirs) {
				String[] sortParts = propAndDir.split(" ");
				Class<? extends MeshVertex> currentMapping = vertexClass;
				String sanitizedPart = sanitizeInput(sortParts[0]);
				String[] pathParts = sanitizedPart.split("\\.");
				if (pathParts.length > 1) {
					text.append(", ");
					for (String pathPart: pathParts) {
						Map<String, Triple<String, Class<? extends MeshVertex>, String>> relation = GraphRelationships.findRelation(currentMapping);
						if (relation != null 
								&& !sanitizedPart.endsWith(pathPart)
								&& (relation.containsKey(pathPart) 
										|| (relation.containsKey("*")))) {
							Triple<String, Class<? extends MeshVertex>, String> relationMapping = relation.get(pathPart) != null 
									? relation.get(pathPart) 
									: relation.get("*");
							// TODO custom edge fetch does not work in OSQLSynchQuery as of v3.1.11
							if (StringUtils.isNotBlank(relationMapping.getRight())) {
								text.append("outE('");
								text.append(relationMapping.getLeft());
								text.append("')[");
								text.append(relationMapping.getRight());
								text.append("='");
								text.append(ContainerType.PUBLISHED.getCode()); // TODO support more edge types, not only published
								text.append("'].inV()");
							} else {
								text.append("out('");
								text.append(relationMapping.getLeft());
								text.append("')");
							}
							text.append("[0].");
							currentMapping = relationMapping.getMiddle();
						} else {
							text.append("`");
							text.append(pathPart);
							text.append("`");							
						}
					}
					text.append(" as ");
					text.append("`");
					text.append(sanitizedPart.replace(".", "-"));
					text.append("`");
				}
			}
		}
		text.append(" ");
		text.append(QUERY_FROM);
		text.append(OrientBaseGraph.encodeClassName(vertexClass.getSimpleName()));			

		final List<Object> queryParams = manageFilters(text);
		if (!((OrientBaseGraph) graph).isUseClassForVertexLabel())
			manageLabels(queryParams.size() > 0, text);

		if (this.propsAndDirs != null && this.propsAndDirs.length > 0) {
			text.append(ORDERBY);
			// format: path.name direction, e.g. 'fields.fullname desc'
			for (String propAndDir : this.propsAndDirs) {
				if (!propAndDir.equals(propsAndDirs[0])) {
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

		final OSQLSynchQuery<OIdentifiable> query = new OSQLSynchQuery<OIdentifiable>(text.toString());

		if (fetchPlan != null)
			query.setFetchPlan(fetchPlan);

		return new OrientElementIterable<Vertex>(((OrientBaseGraph) graph),
				((OrientBaseGraph) graph).getRawGraph().query(query, queryParams.toArray()));
	}
	
	private static final String sanitizeInput(String input) {
		// TODO replace with pattern matching for more anti-sql-injection sanitizing
		return input.replaceAll(";", StringUtils.EMPTY);
	}
}
