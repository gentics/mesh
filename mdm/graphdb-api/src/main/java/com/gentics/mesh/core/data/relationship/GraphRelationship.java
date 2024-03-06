package com.gentics.mesh.core.data.relationship;

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

	/**
	 * Constructor
	 * 
	 * @param edgeName an edge label
	 * @param relatedVertexClass a vertex class name, where the edge goes to
	 * @param edgeFieldName name of an edge property, that is being used in the filtering. If null/empty, no edge property filtering used 
	 * @param defaultEdgeFieldFilterValue default value for the edge property filtering. Unused, if edgeFieldName is null/empty.
	 */
	public GraphRelationship(String edgeName, Class<?> relatedVertexClass, String edgeFieldName,
			String defaultEdgeFieldFilterValue) {
		this.edgeName = edgeName;
		this.relatedVertexClass = relatedVertexClass;
		this.edgeFieldName = edgeFieldName;
		this.defaultEdgeFieldFilterValue = defaultEdgeFieldFilterValue;
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
}
