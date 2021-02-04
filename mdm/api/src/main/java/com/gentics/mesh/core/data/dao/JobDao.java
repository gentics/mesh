package com.gentics.mesh.core.data.dao;

import java.util.function.Predicate;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.job.HibJob;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.rest.job.JobResponse;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.parameter.PagingParameters;

import io.reactivex.Completable;

public interface JobDao extends DaoTransformable<HibJob, JobResponse> {

	/**
	 * Find all jobs.
	 * 
	 * @return
	 */
	Result<? extends HibJob> findAll();

	/**
	 * Load a page of jobs.
	 * 
	 * @param ac
	 * @param pagingInfo
	 * @return
	 */
	Page<? extends HibJob> findAll(InternalActionContext ac, PagingParameters pagingInfo);

	/**
	 * Load a page of jobs.
	 * 
	 * @param ac
	 * @param pagingInfo
	 * @param extraFilter
	 * @return
	 */
	Page<? extends HibJob> findAll(InternalActionContext ac, PagingParameters pagingInfo, Predicate<HibJob> extraFilter);

	/**
	 * Find the job by uuid.
	 * 
	 * @param uuid
	 * @return
	 */
	HibJob findByUuid(String uuid);

	/**
	 * Find the job by name.
	 * 
	 * @param name
	 * @return
	 */
	HibJob findByName(String name);

	/**
	 * Load the job by uuid.
	 * 
	 * @param ac
	 * @param uuid
	 * @param perm
	 * @param errorIfNotFound
	 * @return
	 */
	HibJob loadObjectByUuid(InternalActionContext ac, String uuid, InternalPermission perm, boolean errorIfNotFound);

	/**
	 * Load the job by uuid.
	 * 
	 * @param uuid
	 * @param errorIfNotFound
	 * @return
	 */
	HibJob loadObjectByUuidNoPerm(String uuid, boolean errorIfNotFound);

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
	 * Compute the total count of jobs.
	 * 
	 * @return
	 */
	long computeCount();

	/**
	 * Enqueue the branch migration.
	 * 
	 * @param creator
	 * @param branch
	 * @return
	 */
	HibJob enqueueBranchMigration(HibUser creator, HibBranch branch);

	/**
	 * Return the etag of the job.
	 */
	String getETag(HibJob job, InternalActionContext ac);

	/**
	 * Return the api path of the job.
	 * 
	 * @param job
	 * @param ac
	 * @return
	 */
	String getAPIPath(HibJob job, InternalActionContext ac);

	/**
	 * Transform the job to rest
	 * 
	 * @param job
	 * @param ac
	 * @param level
	 * @param languageTags
	 * @return
	 */
	JobResponse transformToRestSync(HibJob job, InternalActionContext ac, int level, String... languageTags);

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
	 * Clear all jobs.
	 */
	void clear();

	/**
	 * Process all jobs.
	 * 
	 * @return
	 */
	Completable process();


}
