package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.job.HibJob;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.rest.branch.BranchResponse;
import com.gentics.mesh.event.EventQueueBatch;

/**
 * DAO for {@link HibBranch}.
 */
public interface BranchDao extends DaoTransformable<HibBranch, BranchResponse>, Dao<HibBranch>, RootDao<HibProject, HibBranch> {

	/**
	 * Assign the given schema version to the branch and queue a job which will trigger the migration.
	 * 
	 * @param branch
	 * @param user
	 * @param schemaVersion
	 * @param batch
	 * @return Job which was created to trigger the migration or null if no job was created because the version has already been assigned before
	 */
	HibJob assignSchemaVersion(HibBranch branch, HibUser user, HibSchemaVersion schemaVersion, EventQueueBatch batch);

	/**
	 * Assign the given microschema version to the branch and queue a job which executes the migration.
	 * 
	 * @param branch
	 * @param user
	 * @param microschemaVersion
	 * @param batch
	 * @return Job which has been created if the version has not yet been assigned. Otherwise null will be returned.
	 */
	HibJob assignMicroschemaVersion(HibBranch branch, HibUser user, HibMicroschemaVersion microschemaVersion, EventQueueBatch batch);

	/**
	 * Find the existing branch for the name, before using it in an update.
	 * 
	 * @param name
	 * @return
	 */
	HibBranch findConflictingBranch(HibBranch branch, String name);
	
	/**
	 * Return the API path for the branch.
	 * 
	 * @param element
	 * @param ac
	 * @return
	 */
	String getAPIPath(HibBranch element, InternalActionContext ac);

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
