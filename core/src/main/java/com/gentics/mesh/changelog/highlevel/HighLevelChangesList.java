package com.gentics.mesh.changelog.highlevel;

import java.util.List;

import javax.inject.Singleton;

import com.gentics.mesh.core.data.changelog.HighLevelChange;

/**
 * List of high level changes which can be applied once the low level changelog entries have been applied.
 */
@Singleton
public interface HighLevelChangesList {

	/**
	 * The list of HighLevelChange to be applied
	 * @return
	 */
	List<HighLevelChange> getList();
}
