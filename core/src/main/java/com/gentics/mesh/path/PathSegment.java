package com.gentics.mesh.path;

import com.gentics.mesh.core.data.node.Node;

public class PathSegment {

	private Node node;

	private String languageTag;

	public PathSegment(Node node) {
		this.node = node;
	}

	public String getLanguageTag() {
		return languageTag;
	}

	public Node getNode() {
		return node;
	}

}
