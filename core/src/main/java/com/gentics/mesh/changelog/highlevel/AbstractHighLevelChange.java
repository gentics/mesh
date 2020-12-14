package com.gentics.mesh.changelog.highlevel;

import com.gentics.mesh.core.data.changelog.HighLevelChange;

/**
 * Default abstract implementation for an {@link HighLevelChange} changelog entry.
 */
public abstract class AbstractHighLevelChange implements HighLevelChange {

	@Override
	public void apply() {

	}

	@Override
	public void applyNoTx() {

	}

}
