package com.gentics.mesh.cache;

import com.gentics.mesh.core.rest.common.RestModel;

/**
 * Cache status info.
 */
public class CacheStatusModel implements RestModel {

	private String name;
	private long currentSizeInUnits;
	private long maxSizeInUnits;
	private String setup;
	
	public CacheStatusModel(String name, long currentSizeInUnits, long maxSizeInUnits, String setup) {
		this.name = name;
		this.currentSizeInUnits = currentSizeInUnits;
		this.maxSizeInUnits = maxSizeInUnits;
		this.setup = setup;
	}

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public long getCurrentSizeInUnits() {
		return currentSizeInUnits;
	}
	public void setCurrentSizeInUnits(long currentSizeInUnits) {
		this.currentSizeInUnits = currentSizeInUnits;
	}
	public long getMaxSizeInUnits() {
		return maxSizeInUnits;
	}
	public void setMaxSizeInUnits(long maxSizeInUnits) {
		this.maxSizeInUnits = maxSizeInUnits;
	}
	public String getSetup() {
		return setup;
	}
	public void setSetup(String setup) {
		this.setup = setup;
	}
}
