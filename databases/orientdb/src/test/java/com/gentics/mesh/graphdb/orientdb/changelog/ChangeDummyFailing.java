package com.gentics.mesh.graphdb.orientdb.changelog;

import com.gentics.mesh.changelog.AbstractChange;

public class ChangeDummyFailing extends AbstractChange {

	@Override
	public String getUuid() {
		return "424FA7436B6541269E6CE90C8C3D812D";
	}

	@Override
	public String getName() {
		return "Failing change";
	}

	@Override
	public String getDescription() {
		return "A test which fails with an NPE";
	}

	@Override
	public void actualApply() {
		throw new NullPointerException("testing error handling");
	}

}
