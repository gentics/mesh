package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.branch.HibBranchMicroschemaVersion;
import com.gentics.mesh.core.data.branch.HibBranchSchemaVersion;
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
	 * Return the currently set latest branch of the project.
	 *
	 * @param project
	 * @return
	 */
	HibBranch getLatestBranch(HibProject project);

	/**
	 * Return the initial branch of the project.
	 *
	 * @param project
	 * @return
	 */
	HibBranch getInitialBranch(HibProject project);

	/**
	 * Locate the branch with the given name or uuid. Fallback to the latest branch if the given branch could not be found.
	 *
	 * @param project
	 * @param branchNameOrUuid
	 * @return
	 */
	HibBranch findBranchOrLatest(HibProject project, String branchNameOrUuid);

	/**
	 * Find the branch with the given name or uuid that exists in the project.
	 *
	 * @param project
	 * @param branchNameOrUuid
	 * @return
	 */
	HibBranch findBranch(HibProject project, String branchNameOrUuid);

	/**
	 * Find the branch schema edge for the given branch and version.
	 *
	 * @param branch
	 * @param schemaVersion
	 * @return Found edge between branch and version
	 */
	HibBranchSchemaVersion findBranchSchemaEdge(HibBranch branch, HibSchemaVersion schemaVersion);


	/**
	 * Find the branch microschema edge for the given branch and version.
	 *
	 * @param branch
	 * @param microschemaVersion
	 * @return Found edge between branch and version
	 */
	HibBranchMicroschemaVersion findBranchMicroschemaEdge(HibBranch branch, HibMicroschemaVersion microschemaVersion);
}
