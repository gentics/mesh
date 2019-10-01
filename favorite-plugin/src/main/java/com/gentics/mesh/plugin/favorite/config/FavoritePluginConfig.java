package com.gentics.mesh.plugin.favorite.config;

import java.util.List;

public class FavoritePluginConfig {
	private List<String> projects;

	private int maxEntriesPerType = -1;
	
	private boolean uniqueEntries = true;

	public List<String> getProjects() {
		return projects;
	}

	public void setProjects(List<String> projects) {
		this.projects = projects;
	}
	
	public int getMaxEntriesPerType() {
		return maxEntriesPerType;
	}

	public void setMaxEntriesPerType(int maxEntriesPerType) {
		this.maxEntriesPerType = maxEntriesPerType;
	}

	public boolean hasUniqueEntries() {
		return uniqueEntries;
	}

	public void setUniqueEntries(boolean uniqueEntries) {
		this.uniqueEntries = uniqueEntries;
	}
	
}
