package com.gentics.mesh.changelog.changes;

import com.gentics.mesh.changelog.AbstractChange;

public class ChangeReindex3 extends AbstractChange {

	@Override
	public String getUuid() {
		return "093BEFB47FA4476FBE37FD27C613F7AA";
	}

	@Override
	public String getName() {
		return "Invoke full node reindex";
	}

	@Override
	public String getDescription() {
		return "Reindex all nodes due to new publish field in node documents";
	}

	@Override
	public void apply() {
		addFullReindexEntry("node");
	}

}
