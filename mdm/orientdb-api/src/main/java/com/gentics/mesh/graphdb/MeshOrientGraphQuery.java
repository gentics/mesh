package com.gentics.mesh.graphdb;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.apache.commons.lang.StringUtils;

import com.gentics.mesh.cache.TotalsCache;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.relationship.GraphRelationship;
import com.gentics.mesh.core.data.relationship.GraphRelationships;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.impls.orient.OrientGraphQuery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A query with sorting capabilities.
 * 
 * @author plyhun
 *
 * @param <T> result element type
 * @param <P> extra parameter type
 */
abstract class MeshOrientGraphQuery<T extends Element, P> extends OrientGraphQuery {

	protected static final Logger log = LoggerFactory.getLogger(MeshOrientGraphQuery.class);

	protected static final String QUERY_SELECT = "select ";
	protected static final String QUERY_FROM = "from ";

	protected Optional<String> maybeCustomFilter = Optional.empty();
	protected String[] orderPropsAndDirs = null;
	protected Direction relationDirection = Direction.OUT;

	protected final Class<?> vertexClass;
	protected final Optional<TotalsCache> maybeTotalsCache;

	protected MeshOrientGraphQuery(Graph iGraph, Class<?> vertexClass, Optional<TotalsCache> maybeTotalsCache) {
		super(iGraph);
		this.vertexClass = vertexClass;
		this.maybeTotalsCache = maybeTotalsCache;
	}

	public MeshOrientGraphQuery<T, P> filter(Optional<String> maybeCustomFilter) {
		this.maybeCustomFilter = Optional.ofNullable(maybeCustomFilter).flatMap(Function.identity());
		return this;
	}

	/**
	 * A shortcut method for multiple {@link OrientGraphQuery#has(String, Object) calls}
	 * 
	 * @param key
	 * @param value
	 * @return
	 */
	public MeshOrientGraphQuery<T, P> hasAll(final String[] key, final Object[] value) {
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
	public MeshOrientGraphQuery<T, P> relationDirection(Direction relationDirection) {
		this.relationDirection = relationDirection;
		return this;
	}

	/**
	 * Fetch the results of this query.
	 * 
	 * @param propsAndDirs 
	 * @param extraParam
	 * @return
	 */
	public abstract Iterable<T> fetch(P extraParam);

	/**
	 * Fetch the results of this query.
	 * 
	 * @param propsAndDirs 
	 * @param extraParam
	 * @return
	 */
	public abstract long count(P extraParam);

	/**
	 * Build core query part, with no pagination and ordering.
	 * 
	 * @param text a builder to fill the query.
	 * @return query params
	 */
	protected abstract List<Object> buildCoreQuery(StringBuilder text, P extraParam);

	/**
	 * Sanitize the 'orderBy' input against
	 * 1. chars disallowed in the field naming
	 * 2. SQL injection
	 * 
	 * @param input
	 * @return
	 */
	protected static final String sanitizeInput(String input) {
		// Keep match opposite to FieldSchemaContainer.NAME_REGEX!
		return input.replaceAll("[^.@a-zA-Z0-9_-]", StringUtils.EMPTY);
	}

	/**
	 * Check if field name contains an edge reference, and escape it accordingly.
	 * 
	 * @param sb
	 * @param fieldName
	 * @return
	 */
	protected static final StringBuilder escapeFieldNameIfRequired(StringBuilder sb, String fieldName) {
		boolean escape = !fieldName.contains("(") && !fieldName.contains(")");
		if (escape) {
			sb.append("`");
		}
		sb.append(fieldName);
		if (escape) {
			sb.append("`");
		}
		return sb;
	}

	protected void buildOrderEntityRequest(StringBuilder text, boolean isEdgeRequest, boolean useEdgeFilters) {
		if (orderPropsAndDirs != null && orderPropsAndDirs.length > 0) {
			// format: path.name direction, e.g. 'fields.fullname desc'
			for (String propAndDir : orderPropsAndDirs) {
				String[] sortParts = propAndDir.split(" ");
				Class<?> currentMapping = vertexClass;
				String sanitizedPart = sanitizeInput(sortParts[0]);
				String[] pathParts = sanitizedPart.split("\\.");
				Map<String, GraphRelationship> relation = GraphRelationships.findRelation(currentMapping);
				for (int i = 0; i < pathParts.length; i++) {
					String pathPart = pathParts[i];
					if (relation != null && !sanitizedPart.endsWith(pathPart)
							&& (relation.containsKey(pathPart) || (relation.containsKey("*")))) {
						GraphRelationship relationMapping = relation.get(pathPart) != null ? relation.get(pathPart) : relation.get("*");
						if (useEdgeFilters && relationMapping != null) {
							if (MeshVertex.UUID_KEY.equals(relationMapping.getEdgeName())) {
								String partName = pathParts.length > 1 ? pathParts[1] : pathPart;
								text.append(" LET $");
								text.append(partName);
								text.append(relationMapping.getRelatedVertexClass().getSimpleName());
								text.append(" = ");
								text.append("(SELECT `");
								text.append(pathParts.length > 1 ? pathParts[1] : pathPart); // TODO check this
								text.append("` FROM ");
								text.append(relationMapping.getRelatedVertexClass().getSimpleName());
								text.append(" WHERE uuid = $parent.$current.");
								if (i < 1 && isEdgeRequest) {
									text.append(relationDirection.name().toLowerCase());
									text.append("V().");
								}
								escapeFieldNameIfRequired(text, relationMapping.getEdgeFieldName());
								text.append(")");
								// For UUID-based relations we need no more iterations
								break;
							}
						}
					}
				}
			}
		}
		text.append(" ");
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
	protected void buildOrderFieldRequest(StringBuilder text, boolean isEdgeRequest, boolean useEdgeFilters) {
		if (orderPropsAndDirs != null && orderPropsAndDirs.length > 0) {
			// format: path.name direction, e.g. 'fields.fullname desc'
			Direction vertexLookupDir = isEdgeRequest ? relationDirection.opposite() : relationDirection;
			Direction vertexLookupDirOpposite = vertexLookupDir.opposite();
			for (String propAndDir : orderPropsAndDirs) {
				String[] sortParts = propAndDir.split(" ");
				Class<?> currentMapping = vertexClass;
				String sanitizedPart = sanitizeInput(sortParts[0]);
				String[] pathParts = sanitizedPart.split("\\.");
				text.append(", ");
				Map<String, GraphRelationship> relation = GraphRelationships.findRelation(currentMapping);
				for (int i = 0; i < pathParts.length; i++) {
					String pathPart = pathParts[i];
					if (relation != null && !sanitizedPart.endsWith(pathPart)
							&& (relation.containsKey(pathPart) || (relation.containsKey("*")))) {
						GraphRelationship relationMapping = relation.get(pathPart) != null ? relation.get(pathPart) : relation.get("*");
						if (useEdgeFilters && relationMapping != null) {
							if (MeshVertex.UUID_KEY.equals(relationMapping.getEdgeName())) {
								String partName = pathParts.length > 1 ? pathParts[1] : pathPart;
								text.append("$");
								text.append(partName);
								text.append(relationMapping.getRelatedVertexClass().getSimpleName());
								text.append(".");
								text.append(partName);
								text.append("[0]");
								// For UUID-based relations we need no more iterations
								break;
							} else if (StringUtils.isBlank(relationMapping.getEdgeName())) {
								if (i < 1 && isEdgeRequest) {
									text.append(relationDirection.name().toLowerCase());
									text.append("V().");
								}
								text.append("(");
								escapeFieldNameIfRequired(text, relationMapping.getEdgeFieldName());
								text.append(")");
							} else {
								if (i < 1 && isEdgeRequest) {
									text.append(relationDirection.name().toLowerCase());
									text.append("V().");
								}
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
							}
						} else {
							if (i < 1 && isEdgeRequest) {
								text.append(relationDirection.name().toLowerCase());
								text.append("V().");
							}
							text.append(vertexLookupDir.name().toLowerCase());
							text.append("(");
							escapeFieldNameIfRequired(text, relationMapping.getEdgeName());
							text.append(")");
						}
						text.append("[0].");
						currentMapping = relationMapping.getRelatedVertexClass();
					} else if (i == 1 && "fields".equals(pathParts[0]) && pathParts.length > 2) {
						// skip the schema name
						continue;
					} else {
						if (i < 1 && isEdgeRequest) {
							text.append(relationDirection.name().toLowerCase());
							text.append("V().");
						}
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

	public String[] getOrderPropsAndDirs() {
		return orderPropsAndDirs;
	}

	public void setOrderPropsAndDirs(String[] orderPropsAndDirs) {
		this.orderPropsAndDirs = orderPropsAndDirs;
	}
}
