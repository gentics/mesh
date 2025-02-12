package com.gentics.mesh.changelog;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.changelog.highlevel.HighLevelChangesList;
import com.gentics.mesh.changelog.highlevel.change.ExtractPlainText;
import com.gentics.mesh.changelog.highlevel.change.SetAdminUserFlag;
import com.gentics.mesh.core.data.changelog.HighLevelChange;

/**
 * High level changelog list for mesh hibernate
 */
@Singleton
public class HibernateHighLevelChangesList extends HighLevelChangesList {

	@Inject
	public HibernateHighLevelChangesList(ExtractPlainText plainText, SetAdminUserFlag setAdminUserFlag) {
		super(plainText, setAdminUserFlag);
	}

	@Override
	public List<HighLevelChange> getList() {
		List<HighLevelChange> changeList = new ArrayList<>(super.getList());

		// ADD NEW CHANGES HERE!
		// WARNING!
		// Only add changes when absolutely needed. Try to avoid changelog entries since those would require a offline update.
		// Instead it is recommended to write changes in a way so that those can be applied on the fly.
		// WARNING!
		return changeList;
	}
}
