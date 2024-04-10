package com.gentics.mesh.core.data.dao;

import java.time.ZonedDateTime;
import java.util.function.Predicate;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.job.HibJob;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.rest.job.JobResponse;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.parameter.PagingParameters;

/**
 * DAO for {@link HibJob}.
 */
public interface JobDao extends DaoGlobal<HibJob>, DaoTransformable<HibJob, JobResponse> {
	
	/**
	 * Find all elements and return a paged result. No permission check will be performed.
	 * 
	 * @param ac
	 *            action context
	 * @param pagingInfo
	 *            Paging information object that contains page options
	 * @return
	 */
	default Page<? extends HibJob> findAllNoPerm(InternalActionContext ac, PagingParameters pagingInfo) {
		return findAllNoPerm(ac, pagingInfo, null);
	}

	/**
	 * Find all elements and return a paged result. No permission check will be performed.
	 * 
	 * @param ac
	 *            action context
	 * @param pagingInfo
	 *            Paging information object that contains page options
	 * @param extraFilter
	 *            Additional filter to be applied
	 * @return
	 */
	Page<? extends HibJob> findAllNoPerm(InternalActionContext ac, PagingParameters pagingInfo, Predicate<HibJob> extraFilter);

	/**
	 * Enqueue the microschema/micronode migration.
	 * 
	 * @param creator
	 * @param branch
	 * @param fromVersion
	 * @param toVersion
	 * @return
	 */
	HibJob enqueueMicroschemaMigration(HibUser creator, HibBranch branch, HibMicroschemaVersion fromVersion, HibMicroschemaVersion toVersion);

	/**
	 * Enqueue the branch migration.
	 * 
	 * @param creator
	 * @param branch
	 * @param fromVersion
	 * @param toVersion
	 * @return
	 */
	HibJob enqueueBranchMigration(HibUser creator, HibBranch branch, HibSchemaVersion fromVersion, HibSchemaVersion toVersion);

	/**
	 * Enqueue the schema/node migration.
	 * 
	 * @param creator
	 * @param branch
	 * @param fromVersion
	 * @param toVersion
	 * @return
	 */
	HibJob enqueueSchemaMigration(HibUser creator, HibBranch branch, HibSchemaVersion fromVersion, HibSchemaVersion toVersion);

	/**
	 * Enqueue the branch migration.
	 * 
	 * @param creator
	 * @param branch
	 * @return
	 */
	HibJob enqueueBranchMigration(HibUser creator, HibBranch branch);

	/**
	 * Update the job.
	 * 
	 * @param job
	 * @param ac
	 * @param batch
	 * @return
	 */
	boolean update(HibJob job, InternalActionContext ac, EventQueueBatch batch);

	/**
	 * Delete the job.
	 * 
	 * @param job
	 * @param bac
	 */
	void delete(HibJob job, BulkActionContext bac);

	/**
	 * Create a new job.
	 * 
	 * @param ac
	 * @param batch
	 * @param uuid
	 * @return
	 */
	HibJob create(InternalActionContext ac, EventQueueBatch batch, String uuid);

	/**
	 * Enqueue a project version purge job that is limited to the given date.
	 * 
	 * @param user
	 * @param project
	 * @param before
	 * @return
	 */
	HibJob enqueueVersionPurge(HibUser user, HibProject project, ZonedDateTime before);

	/**
	 * Enqueue a project version purge job.
	 * @param user
	 * @param project
	 * @return
	 */
	HibJob enqueueVersionPurge(HibUser user, HibProject project);

	/**
	 * Enqueue a consistency check job.
	 * @param user current user
	 * @param repair true to repair consistencies
	 * @return enqueued job
	 */
	HibJob enqueueConsistencyCheck(HibUser user, boolean repair);

	/**
	 * Purge all failed jobs from the job root.
	 */
	void purgeFailed();

	/**
	 * Delete the jobs referencing the project.
	 * @param project
	 */
	void deleteByProject(HibProject project);

	/**
	 * Delete all jobs.
	 */
	void clear();
}
