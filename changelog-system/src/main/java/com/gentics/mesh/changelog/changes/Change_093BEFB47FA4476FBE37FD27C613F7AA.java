package com.gentics.mesh.changelog.changes;

import com.gentics.mesh.changelog.AbstractChange;

public class Change_093BEFB47FA4476FBE37FD27C613F7AA extends AbstractChange {

	@Override
	public String getName() {
		return "Invoke full node reindex";
	}

	@Override
	public String getDescription() {
		return "Reindex all noes due to new publish field in node documents";
	}

	@Override
	public void apply() {
		addFullReindexEntry("node");
	}

}
