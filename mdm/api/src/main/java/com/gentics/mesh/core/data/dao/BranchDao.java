package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.branch.Branch;
import com.gentics.mesh.core.data.branch.BranchMicroschemaVersion;
import com.gentics.mesh.core.data.branch.BranchSchemaVersion;
import com.gentics.mesh.core.data.job.Job;
import com.gentics.mesh.core.data.project.Project;
import com.gentics.mesh.core.data.schema.MicroschemaVersion;
import com.gentics.mesh.core.data.schema.SchemaVersion;
import com.gentics.mesh.core.data.user.User;
import com.gentics.mesh.core.rest.branch.BranchResponse;
import com.gentics.mesh.event.EventQueueBatch;

/**
 * DAO for {@link Branch}.
 */
public interface BranchDao extends DaoTransformable<Branch, BranchResponse>, Dao<Branch>, RootDao<Project, Branch> {

	/**
	 * Assign the given schema version to the branch and queue a job which will trigger the migration.
	 * 
	 * @param branch
	 * @param user
	 * @param schemaVersion
	 * @param batch
	 * @return Job which was created to trigger the migration or null if no job was created because the version has already been assigned before
	 */
	Job assignSchemaVersion(Branch branch, User user, SchemaVersion schemaVersion, EventQueueBatch batch);

	/**
	 * Assign the given microschema version to the branch and queue a job which executes the migration.
	 * 
	 * @param branch
	 * @param user
	 * @param microschemaVersion
	 * @param batch
	 * @return Job which has been created if the version has not yet been assigned. Otherwise null will be returned.
	 */
	Job assignMicroschemaVersion(Branch branch, User user, MicroschemaVersion microschemaVersion, EventQueueBatch batch);

	/**
	 * Find the existing branch for the name, before using it in an update.
	 * 
	 * @param name
	 * @return
	 */
	Branch findConflictingBranch(Branch branch, String name);
	
	/**
	 * Return the API path for the branch.
	 * 
	 * @param element
	 * @param ac
	 * @return
	 */
	String getAPIPath(Branch element, InternalActionContext ac);

	/**
	 * Create the branch.
	 * 
	 * @param project
	 * @param name
	 * @param user
	 * @param batch
	 * @return
	 */
	Branch create(Project project, String name, User user, EventQueueBatch batch);

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
	Branch create(Project project, String name, User creator, String uuid, boolean setLatest, Branch baseBranch, EventQueueBatch batch);

	/**
	 * Return the currently set latest branch of the project.
	 *
	 * @param project
	 * @return
	 */
	Branch getLatestBranch(Project project);

	/**
	 * Return the initial branch of the project.
	 *
	 * @param project
	 * @return
	 */
	Branch getInitialBranch(Project project);

	/**
	 * Locate the branch with the given name or uuid. Fallback to the latest branch if the given branch could not be found.
	 *
	 * @param project
	 * @param branchNameOrUuid
	 * @return
	 */
	Branch findBranchOrLatest(Project project, String branchNameOrUuid);

	/**
	 * Find the branch with the given name or uuid that exists in the project.
	 *
	 * @param project
	 * @param branchNameOrUuid
	 * @return
	 */
	Branch findBranch(Project project, String branchNameOrUuid);

	/**
	 * Find the branch schema edge for the given branch and version.
	 *
	 * @param branch
	 * @param schemaVersion
	 * @return Found edge between branch and version
	 */
	BranchSchemaVersion findBranchSchemaEdge(Branch branch, SchemaVersion schemaVersion);


	/**
	 * Find the branch microschema edge for the given branch and version.
	 *
	 * @param branch
	 * @param microschemaVersion
	 * @return Found edge between branch and version
	 */
	BranchMicroschemaVersion findBranchMicroschemaEdge(Branch branch, MicroschemaVersion microschemaVersion);
}
