package com.gentics.mesh.etc.config;

/**
 * Native query filtering support definition.
 * 
 * @author plyhun
 *
 */
public enum NativeQueryFiltering {

	/**
	 * Defined by the distinct request
	 */
	ON_DEMAND(2),

	/**
	 * Force native filtering, fail if impossible.
	 */
	ALWAYS(1),

	/**
	 * Force old Java filtering.
	 */
	NEVER(0);

	private final int level;

	private NativeQueryFiltering(int level) {
		this.level = level;
	}

	/**
	 * Get the filtering int value.
	 * 
	 * @return
	 */
	public int getLevel() {
		return level;
	}
}
