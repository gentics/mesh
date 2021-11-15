package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.core.data.HibNodeFieldContainer;

public interface PersistingContentDao extends ContentDao {

	/**
	 * Repair the inconsistency for the given container.
	 * 
	 * @param container
	 * @return
	 */
	public boolean repair(HibNodeFieldContainer container);
}
