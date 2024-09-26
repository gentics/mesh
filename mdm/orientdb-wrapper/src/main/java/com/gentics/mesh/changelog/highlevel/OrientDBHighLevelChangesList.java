package com.gentics.mesh.changelog.highlevel;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.changelog.highlevel.change.AssignLanguagesToProject;
import com.gentics.mesh.changelog.highlevel.change.ExtractPlainText;
import com.gentics.mesh.changelog.highlevel.change.FixNodeVersionOrder;
import com.gentics.mesh.changelog.highlevel.change.RestructureWebrootIndex;
import com.gentics.mesh.changelog.highlevel.change.SetAdminUserFlag;
import com.gentics.mesh.core.data.changelog.HighLevelChange;

/**
 * Orient DB extension to {@link HighLevelChangesList}.
 * 
 * @author plyhun
 *
 */
@Singleton
public class OrientDBHighLevelChangesList extends HighLevelChangesList {

	protected final RestructureWebrootIndex restructureWebroot;

	protected final FixNodeVersionOrder fixNodeVersionOrder;

	protected final AssignLanguagesToProject assignLanguagesToProject;

	@Inject
	public OrientDBHighLevelChangesList(ExtractPlainText plainText, SetAdminUserFlag setAdminUserFlag, RestructureWebrootIndex restructureWebroot, FixNodeVersionOrder fixNodeVersionOrder, AssignLanguagesToProject assignLanguagesToProject) {
		super(plainText, setAdminUserFlag);
		this.restructureWebroot = restructureWebroot;
		this.fixNodeVersionOrder = fixNodeVersionOrder;
		this.assignLanguagesToProject = assignLanguagesToProject;
	}

	@Override
	public List<HighLevelChange> getList() {
		List<HighLevelChange> changeList = new ArrayList<>(super.getList());

		// ADD NEW CHANGES HERE!
		// WARNING!
		// Only add changes when absolutely needed. Try to avoid changelog entries since those would require a offline update.
		// Instead it is recommended to write changes in a way so that those can be applied on the fly.
		// WARNING!
		changeList.add(restructureWebroot);
		changeList.add(fixNodeVersionOrder);
		changeList.add(assignLanguagesToProject);

		return changeList;
	}
}
