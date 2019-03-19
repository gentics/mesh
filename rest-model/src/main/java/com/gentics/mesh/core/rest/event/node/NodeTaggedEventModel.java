package com.gentics.mesh.core.rest.event.node;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.branch.BranchReference;
import com.gentics.mesh.core.rest.event.AbstractMeshEventModel;
import com.gentics.mesh.core.rest.event.EventCauseInfo;
import com.gentics.mesh.core.rest.project.ProjectReference;
import com.gentics.mesh.core.rest.tag.TagReference;
import com.gentics.mesh.core.rest.user.NodeReference;

public class NodeTaggedEventModel extends AbstractMeshEventModel {

	@JsonProperty(required = true)
	@JsonPropertyDescription("Reference of the tag.")
	private TagReference tag;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Branch for which the tagging operation was executed.")
	private BranchReference branch;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Reference to the node that was tagged.")
	private NodeReference node;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Reference to the project involved.")
	private ProjectReference project;

	@JsonCreator
	public NodeTaggedEventModel(String origin, EventCauseInfo cause, MeshEvent event, TagReference tag, BranchReference branch, NodeReference node,
		ProjectReference project) {
		super(origin, cause, event);
		this.tag = tag;
		this.branch = branch;
		this.node = node;
		this.project = project;
	}

	public TagReference getTag() {
		return tag;
	}

	public void setTag(TagReference tag) {
		this.tag = tag;
	}

	public BranchReference getBranch() {
		return branch;
	}

	public void setBranch(BranchReference branch) {
		this.branch = branch;
	}

	public NodeReference getNode() {
		return node;
	}

	public void setNode(NodeReference node) {
		this.node = node;
	}

	public ProjectReference getProject() {
		return project;
	}

	public void setProject(ProjectReference project) {
		this.project = project;
	}

}
