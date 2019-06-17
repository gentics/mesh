package com.gentics.mesh.changelog.highlevel;

import com.gentics.mesh.core.data.changelog.HighLevelChange;

public abstract class AbstractHighLevelChange implements HighLevelChange {

	@Override
	public void apply() {

	}
	
	@Override
	public void applyNoTx() {
		
	}

}
