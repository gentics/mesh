package com.gentics.mesh.core.rest.event.node;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.event.AbstractMeshEventModel;
import com.gentics.mesh.core.rest.event.EventCauseInfo;
import com.gentics.mesh.core.rest.user.NodeReference;

public class NodeMovedEventModel extends AbstractMeshEventModel {

	@JsonProperty(required = true)
	@JsonPropertyDescription("Reference of the source node which was moved.")
	private NodeReference source;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Reference of the target node into which the source node was moved.")
	private NodeReference target;

	@JsonCreator
	public NodeMovedEventModel(String origin, EventCauseInfo cause, MeshEvent event, NodeReference source, NodeReference target) {
		super(origin, cause, event);
		this.source = source;
		this.target = target;
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
