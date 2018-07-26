package com.gentics.mesh.changelog.highlevel;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

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
	public HighLevelChangesList() {
		// TODO Auto-generated constructor stub
	}

	public List<HighLevelChange> getList() {
		List<HighLevelChange> list = new ArrayList<>();
		list.add(restructureWebroot);
		// ADD NEW CHANGES HERE!
		return list;
	}
}
