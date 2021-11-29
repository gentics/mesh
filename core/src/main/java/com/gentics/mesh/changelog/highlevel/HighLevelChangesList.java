package com.gentics.mesh.changelog.highlevel;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.changelog.highlevel.change.ExtractPlainText;
import com.gentics.mesh.changelog.highlevel.change.SetAdminUserFlag;
import com.gentics.mesh.core.data.changelog.HighLevelChange;

/**
 * List of high level changes which can be applied once the low level changelog entries have been applied.
 */
@Singleton
public class HighLevelChangesList {

	@Inject
	public ExtractPlainText plainText;

	@Inject
	public SetAdminUserFlag setAdminUserFlag;

	@Inject
	public HighLevelChangesList() {
	}

	public List<HighLevelChange> getList() {
		return Arrays.asList(
			plainText,
			setAdminUserFlag
		// ADD NEW CHANGES HERE!
		// WARNING!
		// Only add changes when absolutely needed. Try to avoid changelog entries since those would require a offline update.
		// Instead it is recommended to write changes in a way so that those can be applied on the fly.
		// WARNING!
		);
	}
}
