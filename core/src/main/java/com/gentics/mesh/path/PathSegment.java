package com.gentics.mesh.path;

import com.tinkerpop.blueprints.Vertex;

public class PathSegment {

	private Vertex vertex;

	private String languageTag;

	public PathSegment(Vertex vertex, String languageTag) {
		this.vertex = vertex;
		this.languageTag = languageTag;
	}

	public String getLanguageTag() {
		return languageTag;
	}

	public Vertex getVertex() {
		return vertex;
	}

}
