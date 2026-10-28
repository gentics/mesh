package com.gentics.mesh.plugin.favorite.config;

public class FavoriteTypeOptions {

	private int maxEntries = -1;
	private boolean allowDuplicates = true;
	private boolean reorderDuplicates = true;
	private String addToList = "append";


	public int getmaxEntries() {
		return maxEntries;
	}

	public void setmaxEntries(int maxEntries) {
		this.maxEntries = maxEntries;
	}


	public boolean hasAllowDuplicates() {
		return allowDuplicates;
	}

	public void setAllowDuplicates(boolean allowDuplicates) {
		this.allowDuplicates = allowDuplicates;
	}


	public boolean hasReorderDuplicates() {
		return reorderDuplicates;
	}

	public void setReorderDuplicates(boolean reorderDuplicates) {
		this.reorderDuplicates = reorderDuplicates;
	}


	public String getAddToList() {
		return addToList;
	}

	public void setAddToList(String addToList) {
		this.addToList = addToList;
	}

}
