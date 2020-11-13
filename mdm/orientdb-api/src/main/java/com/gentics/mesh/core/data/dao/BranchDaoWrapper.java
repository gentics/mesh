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

// TODO move the contents of this to BranchDao once migration is done
public interface BranchDaoWrapper extends BranchDao, DaoTransformable<HibBranch, BranchResponse>, DaoWrapper<HibBranch> {

	Result<? extends HibBranch> findAll(HibProject project);

	Page<? extends HibBranch> findAll(HibProject project, InternalActionContext ac, PagingParameters pagingInfo);

	Page<? extends HibBranch> findAll(HibProject project, InternalActionContext ac, PagingParameters pagingInfo, Predicate<HibBranch> extraFilter);

	HibBranch loadObjectByUuid(HibProject project, InternalActionContext ac, String uuid, InternalPermission perm);

	HibBranch loadObjectByUuid(HibProject project, InternalActionContext ac, String uuid, InternalPermission perm, boolean errorIfNotFound);

	/**
	 * Find the branch of the project by uuid.
	 * 
	 * @param project
	 * @param uuid
	 */
	HibBranch findByUuid(HibProject project, String uuid);

	HibBranch findByName(HibProject project, String name);

	String getETag(HibBranch branch, InternalActionContext ac);

	String getAPIPath(HibBranch branch, InternalActionContext ac);

	boolean update(HibBranch branch, InternalActionContext ac, EventQueueBatch batch);

	HibBranch create(HibProject project, String name, HibUser user, EventQueueBatch batch);

	HibBranch create(HibProject project, InternalActionContext ac, EventQueueBatch batch, String uuid);

	HibBranch create(HibProject project, String name, HibUser creator, String uuid, boolean setLatest, HibBranch baseBranch, EventQueueBatch batch);

	HibBranch getLatestBranch(HibProject project);

}
