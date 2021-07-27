package com.gentics.mesh.core.data.relationship;

public class GraphRelationship {

	private final String edgeName;
	private final Class<?> relatedVertexClass;
	private final String edgeFieldName;
	private final String defaultEdgeFieldFilterValue;

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
