package com.gentics.mesh.changelog.highlevel;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.changelog.highlevel.change.ExtractPlainText;
import com.gentics.mesh.changelog.highlevel.change.RestructureWebrootIndex;
import com.gentics.mesh.core.data.changelog.HighLevelChange;

/**
 * List of high level changes which can be applied once the low level changelog entries have been applied.
 */
@Singleton
public class HighLevelChangesList {

	@Inject
	public RestructureWebrootIndex restructureWebroot;

	@Inject
	public ExtractPlainText plainText;

	@Inject
	public HighLevelChangesList() {
	}

	public List<HighLevelChange> getList() {
		return Arrays.asList(
			restructureWebroot,
			plainText
		// ADD NEW CHANGES HERE!
		);
	}
}
