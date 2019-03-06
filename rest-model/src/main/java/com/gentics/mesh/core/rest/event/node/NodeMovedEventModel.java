package com.gentics.mesh.core.rest.event.node;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.event.AbstractMeshEventModel;
import com.gentics.mesh.core.rest.user.NodeReference;

public class NodeMovedEventModel extends AbstractMeshEventModel {

	@JsonProperty(required = true)
	@JsonPropertyDescription("Reference of the source node which was moved.")
	NodeReference source;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Reference of the target node into which the source node was moved.")
	NodeReference target;

	public NodeMovedEventModel() {

	}

	public NodeReference getSource() {
		return source;
	}

	public void setSource(NodeReference source) {
		this.source = source;
	}

	public NodeReference getTarget() {
		return target;
	}

	public void setTarget(NodeReference target) {
		this.target = target;
	}

}
