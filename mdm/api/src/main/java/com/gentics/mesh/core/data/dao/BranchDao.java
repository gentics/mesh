package com.gentics.mesh.core.data.dao;

import java.util.function.Predicate;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.rest.branch.BranchResponse;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.parameter.PagingParameters;

/**
 * DAO for {@link HibBranch}.
 */
public interface BranchDao extends DaoTransformable<HibBranch, BranchResponse>, DaoWrapper<HibBranch> {

	/**
	 * Load all branches.
	 * 
	 * @param project
	 * @return
	 */
	Result<? extends HibBranch> findAll(HibProject project);

	/**
	 * Load a page of branches.
	 * 
	 * @param project
	 * @param ac
	 * @param pagingInfo
	 * @return
	 */
	Page<? extends HibBranch> findAll(HibProject project, InternalActionContext ac, PagingParameters pagingInfo);

	/**
	 * Load a page of branches.
	 * 
	 * @param project
	 * @param ac
	 * @param pagingInfo
	 * @param extraFilter
	 * @return
	 */
	Page<? extends HibBranch> findAll(HibProject project, InternalActionContext ac, PagingParameters pagingInfo, Predicate<HibBranch> extraFilter);

	/**
	 * Load the branch of the project.
	 * 
	 * @param project
	 * @param ac
	 * @param uuid
	 * @param perm
	 * @return
	 */
	HibBranch loadObjectByUuid(HibProject project, InternalActionContext ac, String uuid, InternalPermission perm);

	/**
	 * Load the branch by uuid.
	 * 
	 * @param project
	 * @param ac
	 * @param uuid
	 * @param perm
	 * @param errorIfNotFound
	 * @return
	 */
	HibBranch loadObjectByUuid(HibProject project, InternalActionContext ac, String uuid, InternalPermission perm, boolean errorIfNotFound);

	/**
	 * Find the branch of the project by uuid.
	 * 
	 * @param project
	 * @param uuid
	 */
	HibBranch findByUuid(HibProject project, String uuid);

	/**
	 * Return the branch by name.
	 * 
	 * @param project
	 * @param name
	 * @return
	 */
	HibBranch findByName(HibProject project, String name);

	/**
	 * Return the API path for the given branch.
	 * 
	 * @param branch
	 * @param ac
	 * @return
	 */
	String getAPIPath(HibBranch branch, InternalActionContext ac);

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
	 * @param ac
	 * @param batch
	 * @param uuid
	 * @return
	 */
	HibBranch create(HibProject project, InternalActionContext ac, EventQueueBatch batch, String uuid);

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
