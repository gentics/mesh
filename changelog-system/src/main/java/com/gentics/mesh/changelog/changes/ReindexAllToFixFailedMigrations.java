package com.gentics.mesh.changelog.changes;

import com.gentics.mesh.changelog.AbstractChange;

public class ReindexAllToFixFailedMigrations extends AbstractChange {

	@Override
	public String getUuid() {
		return "DC21DF249DC34A11A1DF249DC35A11B3";
	}

	@Override
	public String getName() {
		return "Invoke full reindex";
	}

	@Override
	public String getDescription() {
		return "Reindex all documents";
	}

	@Override
	public void actualApply() {

	}

	@Override
	public boolean requiresReindex() {
		return true;
	}

}
