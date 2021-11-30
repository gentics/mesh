package com.gentics.mesh.changelog.highlevel;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.changelog.highlevel.change.FixNodeVersionOrder;
import com.gentics.mesh.changelog.highlevel.change.RestructureWebrootIndex;
import com.gentics.mesh.core.data.changelog.HighLevelChange;

/**
 * Orient DB extension to {@link HighLevelChangesList}.
 * 
 * @author plyhun
 *
 */
@Singleton
public class OrientDBHighLevelChangesList extends HighLevelChangesList {

	@Inject
	public RestructureWebrootIndex restructureWebroot;

	@Inject
	public FixNodeVersionOrder fixNodeVersionOrder;

	@Inject
	public OrientDBHighLevelChangesList() {
	}

	@Override
	public List<HighLevelChange> getList() {
		return Arrays.asList(
			restructureWebroot,
			plainText,
			fixNodeVersionOrder,
			setAdminUserFlag
		// ADD NEW CHANGES HERE!
		// WARNING!
		// Only add changes when absolutely needed. Try to avoid changelog entries since those would require a offline update.
		// Instead it is recommended to write changes in a way so that those can be applied on the fly.
		// WARNING!
		);
	}
}
