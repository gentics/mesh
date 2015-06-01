package com.gentics.mesh.core.rest.node.response;

import java.util.ArrayList;
import java.util.List;

import com.gentics.mesh.core.rest.common.response.AbstractPropertyContainerModel;
import com.gentics.mesh.core.rest.tag.response.TagResponse;

public class NodeResponse extends AbstractPropertyContainerModel {

	private String parentNodeUuid;

	private List<TagResponse> tags = new ArrayList<>();

	private List<String> children;

	private boolean isContainer;

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

	public List<String> getChildren() {
		return children;
	}

	public void setChildren(List<String> children) {
		this.children = children;
	}

	public boolean isContainer() {
		return isContainer;
	}

	public void setContainer(boolean isContainer) {
		this.isContainer = isContainer;
	}
}
