package com.gentics.mesh.graphdb;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

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
import com.tinkerpop.blueprints.impls.orient.OrientEdge;
import com.tinkerpop.blueprints.impls.orient.OrientElementIterable;
import com.tinkerpop.blueprints.impls.orient.OrientGraphQuery;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;

/**
 * A query with sorting capabilities.
 * 
 * @author plyhun
 *
 */
public class MeshOrientGraphQuery extends OrientGraphQuery {

	protected static final String QUERY_SELECT = "select ";
	protected static final String QUERY_FROM = "from ";

	protected Class<?> vertexClass;
	protected String edgeLabel;
	protected Direction relationDirection = Direction.OUT;

	public MeshOrientGraphQuery(Graph iGraph) {
		super(iGraph);
	}

	/**
	 * A shortcut method for multiple {@link OrientGraphQuery#has(String, Object) calls}
	 * 
	 * @param key
	 * @param value
	 * @return
	 */
	public MeshOrientGraphQuery hasAll(final String[] key, final Object[] value) {
		for (int i = 0; i < key.length; i++) {
			super.has(key[i], value[i]);
		}
		return this;
	}

	/**
	 * Set sorting relation's direction
	 * 
	 * @param relationDirection
	 * @return
	 */
	public MeshOrientGraphQuery relationDirection(Direction relationDirection) {
		this.relationDirection = relationDirection;
		return this;
	}

	/**
	 * Set a vertex class, that will be used as a first label in {@link OrientGraphQuery#labels(String...)}
	 * 
	 * @param vertexClass
	 * @return
	 */
	public MeshOrientGraphQuery vertexClass(Class<?> vertexClass) {
		this.vertexClass = vertexClass;
		return this;
	}

	/**
	 * Set an edge label, that will be used as a first label in {@link OrientGraphQuery#labels(String...)}
	 * 
	 * @param edgeLabel
	 * @return
	 */
	public MeshOrientGraphQuery edgeLabel(String edgeLabel) {
		this.edgeLabel = edgeLabel;
		return this;
	}

	/**
	 * Retrieve the ordered edges
	 * 
	 * @param propsAndDirs sorting parameters, in a form of 'field sortOrder', 'field sortOrder', etc.
	 * @return
	 */
	public Iterable<Edge> edgesOrdered(String[] propsAndDirs) {
		if (limit == 0)
			return Collections.emptyList();

		final StringBuilder text = new StringBuilder(512);

		// First build the fields to retrieve and sort by
		text.append(QUERY_SELECT);
		text.append("*");		
		// Explicit fetch plan is not supported by a newer SQL API, so we use it
		// to tell apart the usage of a new and old API. On the other hand,
		// the old SQL API does not support custom edge filtering.
		buildOrderFieldRequest(text, propsAndDirs, true, fetchPlan == null);
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
			text.append(relationDirection.name().toLowerCase());
			text.append("V().");
			text.append(ElementFrame.TYPE_RESOLUTION_KEY);
			text.append(" = '");
			text.append(vertexClass.getSimpleName());
			text.append("' ");
		}

		// Build the order clause
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

		// Explicit fetch plan is not supported by a newer SQL API, so we use it
		// to tell apart the usage of a new and old API.
		if (fetchPlan != null) {
			final OSQLSynchQuery<OIdentifiable> query = new OSQLSynchQuery<OIdentifiable>(text.toString());

			query.setFetchPlan(fetchPlan);

			return new OrientElementIterable<Edge>(((OrientBaseGraph) graph),
					((OrientBaseGraph) graph).getRawGraph().query(query, queryParams.toArray()));
		} else {
			return () -> StreamSupport.stream(((OrientBaseGraph) graph).getRawGraph().query(text.toString(), queryParams.toArray()), false)
					.map(oresult -> (Edge) new OrientEdgeImpl((OrientBaseGraph) graph, oresult.toElement()))
					.iterator();
		}
	}
	
	public Iterable<Vertex> verticesOrdered(String[] propsAndDirs) {
		if (limit == 0)
			return Collections.emptyList();

		final StringBuilder text = new StringBuilder(512);

		// First build the fields to retrieve and sort by
		text.append(QUERY_SELECT);
		text.append("*");
		// Explicit fetch plan is not supported by a newer SQL API, so we use it
		// to tell apart the usage of a new and old API. On the other hand,
		// the old SQL API does not support custom edge filtering.
		buildOrderFieldRequest(text, propsAndDirs, false, fetchPlan == null);		
		text.append(QUERY_FROM);
		text.append(OrientBaseGraph.encodeClassName(vertexClass.getSimpleName()));

		// Build the query params, including the labels (including the one provided with vertexClass).
		final List<Object> queryParams = manageFilters(text);
		if (!((OrientBaseGraph) graph).isUseClassForVertexLabel())
			manageLabels(queryParams.size() > 0, text);

		// Build the order clause
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
		
		// Explicit fetch plan is not supported by a newer SQL API, so we use it
		// to tell apart the usage of a new and old API.
		if (fetchPlan != null) {
			final OSQLSynchQuery<OIdentifiable> query = new OSQLSynchQuery<OIdentifiable>(text.toString());

			query.setFetchPlan(fetchPlan);

			return new OrientElementIterable<Vertex>(((OrientBaseGraph) graph),
					((OrientBaseGraph) graph).getRawGraph().query(query, queryParams.toArray()));
		} else {
			return () -> StreamSupport.stream(((OrientBaseGraph) graph).getRawGraph().query(text.toString(), queryParams.toArray()), false)
				.map(oresult -> (Vertex) new OrientVertex((OrientBaseGraph) graph, oresult.toElement()))
				.iterator();
		}
	}

	/**
	 * Sanitize the 'orderBy' input against
	 * 1. chars disallowed in the field naming
	 * 2. SQL injection
	 * 
	 * @param input
	 * @return
	 */
	private static final String sanitizeInput(String input) {
		// Keep match opposite to FieldSchemaContainer.NAME_REGEX!
		return input.replaceAll("[^.@a-zA-Z0-9_-]", StringUtils.EMPTY);
	}
	
	/**
	 * Build the request of the fields required for the sorting. Since the sorting may ask for the 
	 * fields hidden in a vertex relation hierarchy, we need to utilize the relation mappings, set by entities.
	 * For instance, the request 'nodeReference.fields.fullname-string desc' on a UserImpl needs to map
	 * 'nodeReference' part through 'HAS_NODE_REFERENCE' edge onto NodeImpl relation, further 'fields' mapped 
	 * through 'HAS_FIELD_CONTAINER' edge onto NodeGraphFieldContainerImpl relation, and the 'fullname-string'
	 * is the property of NodeGraphFieldContainerImpl to sort by.
	 * 
	 *  @see GraphRelationship
	 * 
	 * @param text
	 * @param propsAndDirs
	 * @param isEdgeRequest
	 * @param useEdgeFilters
	 */
	protected void buildOrderFieldRequest(StringBuilder text, String[] propsAndDirs, boolean isEdgeRequest, boolean useEdgeFilters) {
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
						if (useEdgeFilters 
								&& relationMapping != null 
								&& StringUtils.isNotBlank(relationMapping.getEdgeFieldName())) {
							text.append(vertexLookupDir.name().toLowerCase());
							text.append("E('");
							text.append(relationMapping.getEdgeName());
							text.append("')[");
							text.append(relationMapping.getEdgeFieldName());
							text.append("='");
							text.append(relation.get(pathPart) != null ? relationMapping.getDefaultEdgeFieldFilterValue() : pathPart); 
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
