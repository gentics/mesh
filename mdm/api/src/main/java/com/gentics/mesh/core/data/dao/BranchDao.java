package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.rest.branch.BranchResponse;
import com.gentics.mesh.event.EventQueueBatch;

/**
 * DAO for {@link HibBranch}.
 */
public interface BranchDao extends DaoTransformable<HibBranch, BranchResponse>, Dao<HibBranch>, RootDao<HibProject, HibBranch> {
	/**
	 * Return the API path for the branch.
	 * 
	 * @param element
	 * @param ac
	 * @return
	 */
	String getAPIPath(HibBranch element, InternalActionContext ac);

	/**
	 * Update the branch.
	 * 
	 * @param branch
	 * @param ac
	 * @param batch
	 * @return
	 */
	boolean update(HibBranch branch, InternalActionContext ac, EventQueueBatch batch);

	/**
	 * Create the branch.
	 * 
	 * @param project
	 * @param name
	 * @param user
	 * @param batch
	 * @return
	 */
	HibBranch create(HibProject project, String name, HibUser user, EventQueueBatch batch);

	/**
	 * Create the branch.
	 * 
	 * @param project
	 * @param name
	 * @param creator
	 * @param uuid
	 * @param setLatest
	 * @param baseBranch
	 * @param batch
	 * @return
	 */
	HibBranch create(HibProject project, String name, HibUser creator, String uuid, boolean setLatest, HibBranch baseBranch, EventQueueBatch batch);

	/**
	 * Return the latest branch for the project.
	 * 
	 * @param project
	 * @return
	 */
	HibBranch getLatestBranch(HibProject project);
}
