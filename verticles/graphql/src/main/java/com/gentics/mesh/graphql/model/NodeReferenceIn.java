package com.gentics.mesh.graphql.model;

import com.gentics.graphqlfilter.util.Lazy;
import com.gentics.mesh.core.data.node.NodeContent;
import com.gentics.mesh.core.data.node.field.nesting.NodeGraphField;

public class NodeReferenceIn {
	private final NodeContent node;
	private final Lazy<String> fieldName;
	private final Lazy<String> micronodeFieldName;

	public NodeReferenceIn(NodeContent node, NodeGraphField nodeGraphField) {
		this.node = node;
		this.fieldName = new Lazy<>(nodeGraphField::getFieldName);
		this.micronodeFieldName = new Lazy<>(() -> nodeGraphField.getMicronodeFieldName().orElse(null));
	}

	public NodeContent getNode() {
		return node;
	}

	public String getFieldName() {
		return fieldName.get();
	}

	public String getMicronodeFieldName() {
		return micronodeFieldName.get();
	}
}
