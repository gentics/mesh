package com.gentics.mesh.search.impl;

/**
 * POJO for an elasticsearch mapping.
 */
public class Mapping {

	private String type;
	private String index;
	private String analyzer;

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getIndex() {
		return index;
	}

	public void setIndex(String index) {
		this.index = index;
	}

	public String getAnalyzer() {
		return analyzer;
	}

	public void setAnalyzer(String analyzer) {
		this.analyzer = analyzer;
	}
}
