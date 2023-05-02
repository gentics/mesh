package com.gentics.mesh.plugin.favorite.config;


import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.etc.config.env.EnvironmentVariable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FavoritePluginConfig {
	private List<String> projects;
	private Map<String, FavoriteTypeOptions> typeOptions = new HashMap<>();

	public List<String> getProjects() {
		return projects;
	}

	public void setProjects(List<String> projects) {
		this.projects = projects;
	}

	@JsonProperty("types")
	public Map<String, FavoriteTypeOptions> getTypeOptions() {
		return typeOptions;
	}

	@JsonProperty("types")
	public void setTypeOptions(Map<String, FavoriteTypeOptions> typeOptions) {
		this.typeOptions = typeOptions;
	}
}
