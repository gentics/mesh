package com.gentics.mesh.core.data.dao;

import java.time.ZonedDateTime;
import java.util.function.Predicate;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.branch.Branch;
import com.gentics.mesh.core.data.job.Job;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.project.Project;
import com.gentics.mesh.core.data.schema.MicroschemaVersion;
import com.gentics.mesh.core.data.schema.SchemaVersion;
import com.gentics.mesh.core.data.user.User;
import com.gentics.mesh.core.rest.job.JobResponse;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.parameter.PagingParameters;

/**
 * DAO for {@link Job}.
 */
public interface JobDao extends DaoGlobal<Job>, DaoTransformable<Job, JobResponse> {
	
	/**
	 * Find all elements and return a paged result. No permission check will be performed.
	 * 
	 * @param ac
	 *            action context
	 * @param pagingInfo
	 *            Paging information object that contains page options
	 * @return
	 */
	default Page<? extends Job> findAllNoPerm(InternalActionContext ac, PagingParameters pagingInfo) {
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
	Page<? extends Job> findAllNoPerm(InternalActionContext ac, PagingParameters pagingInfo, Predicate<Job> extraFilter);

	/**
	 * Enqueue the microschema/micronode migration.
	 * 
	 * @param creator
	 * @param branch
	 * @param fromVersion
	 * @param toVersion
	 * @return
	 */
	Job enqueueMicroschemaMigration(User creator, Branch branch, MicroschemaVersion fromVersion, MicroschemaVersion toVersion);

	/**
	 * Enqueue the branch migration.
	 * 
	 * @param creator
	 * @param branch
	 * @param fromVersion
	 * @param toVersion
	 * @return
	 */
	Job enqueueBranchMigration(User creator, Branch branch, SchemaVersion fromVersion, SchemaVersion toVersion);

	/**
	 * Enqueue the schema/node migration.
	 * 
	 * @param creator
	 * @param branch
	 * @param fromVersion
	 * @param toVersion
	 * @return
	 */
	Job enqueueSchemaMigration(User creator, Branch branch, SchemaVersion fromVersion, SchemaVersion toVersion);

	/**
	 * Enqueue the branch migration.
	 * 
	 * @param creator
	 * @param branch
	 * @return
	 */
	Job enqueueBranchMigration(User creator, Branch branch);

	/**
	 * Update the job.
	 * 
	 * @param job
	 * @param ac
	 * @param batch
	 * @return
	 */
	boolean update(Job job, InternalActionContext ac, EventQueueBatch batch);

	/**
	 * Delete the job.
	 * 
	 * @param job
	 * @param bac
	 */
	void delete(Job job, BulkActionContext bac);

	/**
	 * Create a new job.
	 * 
	 * @param ac
	 * @param batch
	 * @param uuid
	 * @return
	 */
	Job create(InternalActionContext ac, EventQueueBatch batch, String uuid);

	/**
	 * Enqueue a project version purge job that is limited to the given date.
	 * 
	 * @param user
	 * @param project
	 * @param before
	 * @return
	 */
	Job enqueueVersionPurge(User user, Project project, ZonedDateTime before);

	/**
	 * Enqueue a project version purge job.
	 * @param user
	 * @param project
	 * @return
	 */
	Job enqueueVersionPurge(User user, Project project);

	/**
	 * Enqueue a consistency check job.
	 * @param user current user
	 * @param repair true to repair consistencies
	 * @return enqueued job
	 */
	Job enqueueConsistencyCheck(User user, boolean repair);

	/**
	 * Purge all failed jobs from the job root.
	 */
	void purgeFailed();

	/**
	 * Delete the jobs referencing the project.
	 * @param project
	 */
	void deleteByProject(Project project);

	/**
	 * Delete all jobs.
	 */
	void clear();
}
