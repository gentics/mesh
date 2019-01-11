package com.gentics.mesh.changelog.changes;

import com.gentics.mesh.changelog.AbstractChange;

public class ReindexDatabase extends AbstractChange {
	@Override
	public void actualApply() {
		getDb().reindex();
	}

	@Override
	protected boolean applyInTx() {
		return false;
	}

	@Override
	public String getDescription() {
		return "Repopulates the database indices. This is necessary when upgrading to OrientDB 3." +
			" This change is executed before all other changes to ensure that the changes" +
			" can access the database correctly.";
	}

	@Override
	public String getUuid() {
		return "DCF025827B8F44BBB025827B8FA4BB94";
	}

	@Override
	public String getName() {
		return "Invoke database reindex";
	}
}
