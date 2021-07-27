package com.gentics.mesh.graphdb;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.gentics.mesh.core.data.relationship.GraphRelationship;
import com.gentics.mesh.core.data.relationship.GraphRelationships;
import com.gentics.mesh.madl.frame.ElementFrame;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientBaseGraph;
import com.tinkerpop.blueprints.impls.orient.OrientElementIterable;
import com.tinkerpop.blueprints.impls.orient.OrientGraphQuery;

public class MeshOrientGraphQuery extends OrientGraphQuery {

	protected static final String QUERY_SELECT = "select ";
	protected static final String QUERY_FROM = "from ";

	protected Class<?> vertexClass;
	protected String edgeLabel;
	protected Direction relationDirection = Direction.OUT;

	public MeshOrientGraphQuery(Graph iGraph) {
		super(iGraph);
	}

	public MeshOrientGraphQuery hasAll(final String[] key, final Object[] value) {
		for (int i = 0; i < key.length; i++) {
			super.has(key[i], value[i]);
		}
		return this;
	}

	public MeshOrientGraphQuery relationDirection(Direction relationDirection) {
		this.relationDirection = relationDirection;
		return this;
	}

	public MeshOrientGraphQuery vertexClass(Class<?> vertexClass) {
		this.vertexClass = vertexClass;
		return this;
	}

	public MeshOrientGraphQuery edgeLabel(String edgeLabel) {
		this.edgeLabel = edgeLabel;
		return this;
	}

	public Iterable<Edge> edgesOrdered(String[] propsAndDirs) {
		if (limit == 0)
			return Collections.emptyList();

		final StringBuilder text = new StringBuilder(512);

		// The default SELECT does not support joining linked edges,
		// so some extra engineering is required.
		text.append(QUERY_SELECT);
		text.append("*");		
		buildOrderFieldRequest(text, propsAndDirs, true);
		text.append(" ");
		text.append(QUERY_FROM);
		text.append(OrientBaseGraph.encodeClassName(edgeLabel));
		text.append(" ");

		final List<Object> queryParams = manageFilters(text);
		if (!((OrientBaseGraph) graph).isUseClassForVertexLabel())
			manageLabels(queryParams.size() > 0, text);
		
		if (vertexClass != null) {
			if (hasContainers != null && hasContainers.size() > 0) {
				text.append(QUERY_FILTER_AND);				
			} else {
				text.append(QUERY_WHERE);
			}
			text.append(relationDirection.name().toLowerCase());
			text.append("V().");
			text.append(ElementFrame.TYPE_RESOLUTION_KEY);
			text.append(" = '");
			text.append(vertexClass.getSimpleName());
			text.append("' ");
		}

		if (propsAndDirs != null && propsAndDirs.length > 0) {
			text.append(ORDERBY);
			// format: path.name direction, e.g. 'fields.fullname desc'
			for (String propAndDir : propsAndDirs) {
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

		return new OrientElementIterable<Edge>(((OrientBaseGraph) graph),
				((OrientBaseGraph) graph).getRawGraph().query(query, queryParams.toArray()));
	}

	public Iterable<Vertex> verticesOrdered(String[] propsAndDirs) {
		if (limit == 0)
			return Collections.emptyList();

		final StringBuilder text = new StringBuilder(512);

		// The default SELECT does not support joining linked vertices,
		// so some extra engineering is required.
		text.append(QUERY_SELECT);
		text.append("*");
		buildOrderFieldRequest(text, propsAndDirs, false);		
		text.append(QUERY_FROM);
		text.append(OrientBaseGraph.encodeClassName(vertexClass.getSimpleName()));

		final List<Object> queryParams = manageFilters(text);
		if (!((OrientBaseGraph) graph).isUseClassForVertexLabel())
			manageLabels(queryParams.size() > 0, text);

		if (propsAndDirs != null && propsAndDirs.length > 0) {
			text.append(ORDERBY);
			// format: path.name direction, e.g. 'fields.fullname desc'
			for (String propAndDir : propsAndDirs) {
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
		// Keep match to FieldSchemaContainer.NAME_REGEX!
		return input.replaceAll("[^.a-zA-Z0-9_-]", StringUtils.EMPTY);
	}
	
	protected void buildOrderFieldRequest(StringBuilder text, String[] propsAndDirs, boolean isEdgeRequest) {
		if (propsAndDirs != null && propsAndDirs.length > 0) {
			// format: path.name direction, e.g. 'fields.fullname desc'
			Direction vertexLookupDir = isEdgeRequest ? relationDirection.opposite() : relationDirection;
			Direction vertexLookupDirOpposite = vertexLookupDir.opposite();
			for (String propAndDir : propsAndDirs) {
				String[] sortParts = propAndDir.split(" ");
				Class<?> currentMapping = vertexClass;
				String sanitizedPart = sanitizeInput(sortParts[0]);
				String[] pathParts = sanitizedPart.split("\\.");
				text.append(", ");
				if (isEdgeRequest) {
					text.append(relationDirection.name().toLowerCase() + "V().");
				}
				for (String pathPart : pathParts) {
					Map<String, GraphRelationship> relation = GraphRelationships.findRelation(currentMapping);
					if (relation != null && !sanitizedPart.endsWith(pathPart)
							&& (relation.containsKey(pathPart) || (relation.containsKey("*")))) {
						GraphRelationship relationMapping = relation.get(pathPart) != null ? relation.get(pathPart)
								: relation.get("*");
						// TODO custom edge fetch does not work in OSQLSynchQuery as of v3.1.11
						if (StringUtils.isNotBlank(relationMapping.getEdgeFieldName())) {
							text.append(vertexLookupDir.name().toLowerCase());
							text.append("E('");
							text.append(relationMapping.getEdgeName());
							text.append("')[");
							text.append(relationMapping.getEdgeFieldName());
							text.append("='");
							text.append(relationMapping.getDefaultEdgeFieldFilterValue()); // TODO support more edge
																							// types, not only
																							// published
							text.append("'].");
							text.append(vertexLookupDirOpposite.name().toLowerCase());
							text.append("V()");
						} else {
							text.append(vertexLookupDir.name().toLowerCase());
							text.append("('");
							text.append(relationMapping.getEdgeName());
							text.append("')");
						}
						text.append("[0].");
						currentMapping = relationMapping.getRelatedVertexClass();
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
		text.append(" ");
	}
}
