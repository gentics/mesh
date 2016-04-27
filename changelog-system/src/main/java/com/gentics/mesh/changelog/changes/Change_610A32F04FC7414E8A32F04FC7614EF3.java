package com.gentics.mesh.changelog.changes;

import com.gentics.mesh.changelog.AbstractChange;

public class Change_610A32F04FC7414E8A32F04FC7614EF3 extends AbstractChange {

	@Override
	public String getName() {
		return "Invoke full node reindex";
	}

	@Override
	public String getDescription() {
		return "Reindex all nodes due to fixed displayFieldValue in node documents";
	}

	@Override
	public void apply() {
		addFullReindexEntry("node");
	}

}
