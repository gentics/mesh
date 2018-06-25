package com.gentics.mesh.jmh.model;

import java.util.ArrayList;
import java.util.List;

import com.gentics.mesh.core.rest.common.RestModel;

public class JMHCollection implements RestModel {

	private String version;

	private List<JMHResult> results = new ArrayList<>();

	public JMHCollection() {
	}

	public List<JMHResult> getResults() {
		return results;
	}

	public JMHCollection setResults(List<JMHResult> results) {
		this.results = results;
		return this;
	}

	public JMHCollection add(JMHResult result) {
		results.add(result);
		return this;
	}

	public String getVersion() {
		return version;
	}

	public JMHCollection setVersion(String version) {
		this.version = version;
		return this;
	}
}
