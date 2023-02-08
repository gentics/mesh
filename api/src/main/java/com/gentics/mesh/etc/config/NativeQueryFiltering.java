package com.gentics.mesh.etc.config;

public enum NativeQueryFiltering {

	ON_DEMAND(2),
	ALWAYS(1),
	OFF(0);

	private final int level;

	private NativeQueryFiltering(int level) {
		this.level = level;
	}

	public int getLevel() {
		return level;
	}
}
