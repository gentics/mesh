package com.gentics.mesh.core.data.relationship;

import java.util.Optional;

/**
 * A class for determining the relationship of a vertex class over the edge labels and edge properties
 * 
 * @author plyhun
 *
 */
public class GraphRelationship {

	private final String edgeName;
	private final Class<?> relatedVertexClass;
	private final String edgeFieldName;
	private final String defaultEdgeFieldFilterValue;
	private final Optional<String[]> maybeEdgeLevelFields;
	private final boolean skipMapping;

	/**
	 * Constructor
	 * 
	 * @param edgeName an edge label
	 * @param relatedVertexClass a vertex class name, where the edge goes to
	 * @param edgeFieldName name of an edge property, that is being used in the filtering. If null/empty, no edge property filtering used 
	 * @param defaultEdgeFieldFilterValue default value for the edge property filtering. Unused, if edgeFieldName is null/empty.
	 * @param maybeEdgeLevelFields optional list of entity fields, that are applied on an input edge level.
	 */
	public GraphRelationship(String edgeName, Class<?> relatedVertexClass, String edgeFieldName,
			String defaultEdgeFieldFilterValue, Optional<String[]> maybeEdgeLevelFields) {
		this(edgeName, relatedVertexClass, edgeFieldName, defaultEdgeFieldFilterValue, maybeEdgeLevelFields, false);
	}

	/**
	 * Constructor
	 * 
	 * @param edgeName an edge label
	 * @param relatedVertexClass a vertex class name, where the edge goes to
	 * @param edgeFieldName name of an edge property, that is being used in the filtering. If null/empty, no edge property filtering used 
	 * @param defaultEdgeFieldFilterValue default value for the edge property filtering. Unused, if edgeFieldName is null/empty.
	 * @param maybeEdgeLevelFields optional list of entity fields, that are applied on an input edge level.
	 * @param skipMapping this mapping is artificial and should not be used in either filtering or sorting.
	 */
	public GraphRelationship(String edgeName, Class<?> relatedVertexClass, String edgeFieldName,
			String defaultEdgeFieldFilterValue, Optional<String[]> maybeEdgeLevelFields, boolean skipMapping) {
		this.edgeName = edgeName;
		this.relatedVertexClass = relatedVertexClass;
		this.edgeFieldName = edgeFieldName;
		this.defaultEdgeFieldFilterValue = defaultEdgeFieldFilterValue;
		this.maybeEdgeLevelFields = maybeEdgeLevelFields;
		this.skipMapping = skipMapping;
	}
	/**
	 * Constructor with optional edge fields.
	 * 
	 * @param edgeName an edge label
	 * @param relatedVertexClass a vertex class name, where the edge goes to
	 * @param edgeFieldName name of an edge property, that is being used in the filtering. If null/empty, no edge property filtering used 
	 * @param defaultEdgeFieldFilterValue default value for the edge property filtering. Unused, if edgeFieldName is null/empty.
	 */
	public GraphRelationship(String edgeName, Class<?> relatedVertexClass, String edgeFieldName,
			String defaultEdgeFieldFilterValue) {
		this(edgeName, relatedVertexClass, edgeFieldName, defaultEdgeFieldFilterValue, Optional.empty());
	}
	public String getEdgeName() {
		return edgeName;
	}
	public Class<?> getRelatedVertexClass() {
		return relatedVertexClass;
	}
	public String getEdgeFieldName() {
		return edgeFieldName;
	}
	public String getDefaultEdgeFieldFilterValue() {
		return defaultEdgeFieldFilterValue;
	}
	public Optional<String[]> getMaybeEdgeLevelFields() {
		return maybeEdgeLevelFields;
	}

	public boolean isSkipMapping() {
		return skipMapping;
	}
}
