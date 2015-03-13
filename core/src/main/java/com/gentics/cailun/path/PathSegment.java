package com.gentics.cailun.path;

import org.neo4j.graphdb.Node;

public class PathSegment {

	private Node node;

	private String languageTag;

	public PathSegment(Node node, String languageTag) {
		this.node = node;
		this.languageTag = languageTag;
	}

	public String getLanguageTag() {
		return languageTag;
	}

	public Node getNode() {
		return node;
	}

}
