package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.event.EventQueueBatch;

public interface PersistingContentDao extends ContentDao {

	/**
	 * Repair the inconsistency for the given container.
	 * 
	 * @param container
	 * @return
	 */
	boolean repair(HibNodeFieldContainer container);

	/**
	 * Migrate field container of a node onto the new branch.
	 * 
	 * @param container container to migrate
	 * @param newBranch branch to migrate to
	 * @param node container owning node
	 * @param batch event queue for the notifications
	 * @param setInitial is this branch initial for the project?
	 */
	void migrateContainerOntoBranch(HibNodeFieldContainer container, HibBranch newBranch, HibNode node, EventQueueBatch batch, boolean setInitial);
}
