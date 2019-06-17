package com.gentics.mesh.changelog.changes;

import com.gentics.mesh.changelog.AbstractChange;

public class ChangeReindexAll extends AbstractChange {

	@Override
	public String getUuid() {
		return "B33EF099EF144CE7BEF099EF14BCE788";
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
	public boolean requiresReindex() {
		return true;
	}

}
