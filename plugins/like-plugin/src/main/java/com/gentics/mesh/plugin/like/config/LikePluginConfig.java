package com.gentics.mesh.plugin.like.config;

import java.util.List;

import com.gentics.mesh.etc.config.env.Option;

public class LikePluginConfig implements Option {
	private final static String DEFAULT_CONFIG_FIELD = "likes";

	private List<String> projects;

	private String configField = DEFAULT_CONFIG_FIELD;

	private String templatesPath;

	private boolean caching = true;

	public List<String> getProjects() {
		return projects;
	}

	public void setProjects(List<String> projects) {
		this.projects = projects;
	}

	public String getConfigField() {
		return configField;
	}

	public void setConfigField(String configField) {
		this.configField = configField;
	}

	public String getTemplatesPath() {
		return templatesPath;
	}

	public void setTemplatesPath(String templatesPath) {
		this.templatesPath = templatesPath;
	}

	public boolean isCaching() {
		return caching;
	}

	public void setCaching(boolean caching) {
		this.caching = caching;
	}
}
