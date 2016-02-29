package com.gentics.mesh.changelog;

public abstract class AbstractChange implements Change {

	public abstract void apply();

	@Override
	public boolean isApplied() {
		return false;
	}

}
