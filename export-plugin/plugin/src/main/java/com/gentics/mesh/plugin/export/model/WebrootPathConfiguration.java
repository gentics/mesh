package com.gentics.mesh.plugin.export.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

public class WebrootPathConfiguration {

	@JsonProperty()
	@JsonPropertyDescription("For which project(s) this path is valid for. If null/left blank, it will match for all. You may provide multiple names by separating them with a ',' (comma).")
	public List<String> projects;

	// TODO: Add language support?

	@JsonProperty(required = true)
	@JsonPropertyDescription("The Webroot path to the Node which contains the configuration data.")
	public String path;

	public WebrootPathConfiguration() {
	}

	public List<String> getProjects() {
		return projects;
	}

	public WebrootPathConfiguration setProjects(List<String> projects) {
		this.projects = projects;
		return this;
	}

	public String getPath() {
		return path;
	}

	public WebrootPathConfiguration setPath(String path) {
		this.path = path;
		return this;
	}
}
