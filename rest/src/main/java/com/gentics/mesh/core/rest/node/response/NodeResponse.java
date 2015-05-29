package com.gentics.mesh.core.rest.node.response;

import java.util.ArrayList;
import java.util.List;

import com.gentics.mesh.core.rest.common.response.AbstractPropertyContainerModel;
import com.gentics.mesh.core.rest.tag.response.TagResponse;

public class NodeResponse extends AbstractPropertyContainerModel {

	private String parentNodeUuid;

	private List<TagResponse> tags =  new ArrayList<>();

	private List<NodeResponse> children;

	public NodeResponse() {
	}

	public String getParentNodeUuid() {
		return parentNodeUuid;
	}

	public void setParentNodeUuid(String parentNodeUuid) {
		this.parentNodeUuid = parentNodeUuid;
	}

	public List<TagResponse> getTags() {
		return tags;
	}

	public List<NodeResponse> getChildren() {
		return children;
	}
}
