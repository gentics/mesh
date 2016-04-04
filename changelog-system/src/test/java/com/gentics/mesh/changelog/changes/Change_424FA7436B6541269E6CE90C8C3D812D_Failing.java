package com.gentics.mesh.changelog.changes;

import com.gentics.mesh.changelog.AbstractChange;

public class Change_424FA7436B6541269E6CE90C8C3D812D_Failing extends AbstractChange {

	@Override
	public String getName() {
		return "Failing change";
	}

	@Override
	public String getDescription() {
		return "A test which fails with an NPE";
	}

	@Override
	public void apply() {
		throw new NullPointerException("testing error handling");
	}

}
