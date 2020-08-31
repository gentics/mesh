package com.gentics.mesh.core.data.branch;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.HibCoreElement;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.job.HibJob;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.schema.HibMicroschema;
import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.data.schema.MicroschemaVersion;
import com.gentics.mesh.core.data.schema.SchemaVersion;
import com.gentics.mesh.core.data.tag.HibTag;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.data.user.HibUserTracking;
import com.gentics.mesh.core.rest.branch.BranchReference;
import com.gentics.mesh.core.rest.event.branch.BranchMicroschemaAssignModel;
import com.gentics.mesh.core.rest.event.branch.BranchSchemaAssignEventModel;
import com.gentics.mesh.core.rest.event.branch.BranchTaggedEventModel;
import com.gentics.mesh.core.rest.event.project.ProjectBranchEventModel;
import com.gentics.mesh.core.rest.job.JobStatus;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.event.Assignment;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.parameter.PagingParameters;

public interface HibBranch extends HibCoreElement, HibUserTracking {

	String getName();

	void setName(String string);

	HibUser getCreator();

	HibProject getProject();

	/**
	 * Get whether the branch is active.
	 * 
	 * @return true for active branch
	 */
	boolean isActive();

	/**
	 * Set whether the branch is active.
	 * 
	 * @param active
	 *            true for active
	 * @return Fluent API
	 */
	HibBranch setActive(boolean active);

	/**
	 * Get whether all nodes of the previous branch have been migrated.
	 * 
	 * @return true if all nodes have been migrated
	 */
	boolean isMigrated();

	/**
	 * Set whether all nodes have been migrated.
	 * 
	 * @param migrated
	 *            true if all nodes have been migrated
	 * @return Fluent API
	 */
	HibBranch setMigrated(boolean migrated);

	/**
	 * Return the configured hostname of the branch.
	 * 
	 * @return
	 */
	String getHostname();

	/**
	 * Set the hostname of the branch.
	 * 
	 * @param hostname
	 * @return Fluent API
	 */
	HibBranch setHostname(String hostname);

	/**
	 * Return the ssl flag of the branch.
	 * 
	 * @return
	 */
	Boolean getSsl();

	/**
	 * Set the ssl flag of the branch.
	 * 
	 * @param ssl
	 * @return Fluent API
	 */
	HibBranch setSsl(boolean ssl);

	/**
	 * Return the webroot path prefix.
	 * 
	 * @return
	 */
	String getPathPrefix();

	/**
	 * Set the path prefix.
	 * 
	 * @param pathPrefix
	 * @return Fluent API
	 */
	HibBranch setPathPrefix(String pathPrefix);

	/**
	 * Get whether the branch is the latest branch
	 * 
	 * @return
	 */
	boolean isLatest();

	/**
	 * Make the branch the latest branch of the project
	 * 
	 * @return
	 */
	HibBranch setLatest();

	/**
	 * Get the next Branch.
	 * 
	 * @return next Branch
	 */
	HibBranch getNextBranch();

	/**
	 * Set the next Branch.
	 * 
	 * @param branch
	 *            next Branch
	 * @return Fluent API
	 */
	HibBranch setNextBranch(Branch branch);

	/**
	 * Get the previous Branch.
	 * 
	 * @return previous Branch
	 */
	HibBranch getPreviousBranch();

	/**
	 * Assign the given schema version to the branch and queue a job which will trigger the migration.
	 * 
	 * @param user
	 * @param schemaVersion
	 * @param batch
	 * @return Job which was created to trigger the migration or null if no job was created because the version has already been assigned before
	 */
	HibJob assignSchemaVersion(HibUser user, HibSchemaVersion schemaVersion, EventQueueBatch batch);

	/**
	 * Unassign all schema versions of the given schema from this branch.
	 * 
	 * @param schemaContainer
	 * @return Fluent API
	 */
	HibBranch unassignSchema(HibSchema schemaContainer);

	/**
	 * Check whether a version of this schema container is assigned to this branch.
	 *
	 * @param schema
	 *            schema
	 * @return true iff assigned
	 */
	boolean contains(HibSchema schema);

	/**
	 * Check whether the given schema container version is assigned to this branch.
	 *
	 * @param schemaVersion
	 *            schema container version
	 * @return true if assigned
	 */
	boolean contains(HibSchemaVersion schemaVersion);

	/**
	 * Get an traversal result of all schema container versions.
	 * 
	 * @return
	 */
	Result<? extends SchemaVersion> findAllSchemaVersions();

	/**
	 * Assign the given microschema version to the branch and queue a job which executes the migration.
	 * 
	 * @param user
	 * 
	 * @param microschemaVersion
	 * @param batch
	 * @return Job which has been created if the version has not yet been assigned. Otherwise null will be returned.
	 */
	HibJob assignMicroschemaVersion(HibUser user, HibMicroschemaVersion microschemaVersion, EventQueueBatch batch);

	/**
	 * Unassigns all versions of the given microschema from this branch.
	 * 
	 * @param microschema
	 * @return Fluent API
	 */
	HibBranch unassignMicroschema(HibMicroschema microschema);

	/**
	 * Check whether a version of this microschema container is assigned to this branch.
	 *
	 * @param microschema
	 *            microschema
	 * @return true iff assigned
	 */
	boolean contains(HibMicroschema microschema);

	/**
	 * Check whether the given microschema container version is assigned to this branch.
	 *
	 * @param microschemaVersion
	 *            microschema container version
	 * @return true iff assigned
	 */
	boolean contains(HibMicroschemaVersion microschemaVersion);

	/**
	 * Get an iterable of all microschema container versions.
	 * 
	 * @return Iterable
	 */
	Result<? extends HibMicroschemaVersion> findAllMicroschemaVersions();

	/**
	 * Get an iterable of all latest microschema container versions.
	 * 
	 * @return Iterable
	 */
	Result<? extends HibBranchMicroschemaVersion> findAllLatestMicroschemaVersionEdges();

	/**
	 * Get an iterable over all active schema container versions. An active version is one which still contains {@link NodeGraphFieldContainer}'s or one which
	 * is queued and will soon contain containers due to an executed node migration.
	 * 
	 * @return Iterable
	 */
	Result<? extends HibSchemaVersion> findActiveSchemaVersions();

	/**
	 * Get an iterable over all active microschema container versions. An active version is one which still contains {@link NodeGraphFieldContainer}'s or one
	 * which is queued and will soon contain containers due to an executed node migration.
	 *
	 * @return Iterable
	 */
	Iterable<? extends HibMicroschemaVersion> findActiveMicroschemaVersions();

	/**
	 * Get an iterable of all latest schema container versions.
	 * 
	 * @return Iterable
	 */
	Iterable<? extends HibBranchSchemaVersion> findAllLatestSchemaVersionEdges();

	/**
	 * Assign the branch to a specific project.
	 * 
	 * @param project
	 * @return Fluent API
	 */
	HibBranch setProject(HibProject project);

	/**
	 * Return all schema versions which are linked to the branch.
	 * 
	 * @return
	 */
	Result<? extends HibBranchSchemaVersion> findAllSchemaVersionEdges();

	/**
	 * Return all microschema versions which are linked to the branch.
	 * 
	 * @return
	 */
	Result<? extends HibBranchMicroschemaVersion> findAllMicroschemaVersionEdges();

	/**
	 * Find the branch schema edge for the given version.
	 * 
	 * @param schemaVersion
	 * @return Found edge between branch and version
	 */
	HibBranchSchemaVersion findBranchSchemaEdge(HibSchemaVersion schemaVersion);

	/**
	 * Find the branch microschema edge for the given version.
	 * 
	 * @param microschemaVersion
	 * @return Found edge between branch and version
	 */
	HibBranchMicroschemaVersion findBranchMicroschemaEdge(HibMicroschemaVersion microschemaVersion);

	/**
	 * Find the latest schema version which is assigned to the branch which matches the provided schema container
	 * 
	 * @param schemaContainer
	 * @return Found version or null if no version could be found.
	 */
	HibSchemaVersion findLatestSchemaVersion(HibSchema schemaContainer);

	/**
	 * Find the latest microschema version which is assigned to the branch which matches the provided microschema container
	 * 
	 * @param schemaContainer
	 * @return Found version or null if no version could be found.
	 */
	HibMicroschemaVersion findLatestMicroschemaVersion(HibMicroschema schemaContainer);

	/**
	 * Add the given tag to the list of tags for this branch.
	 * 
	 * @param tag
	 */
	void addTag(HibTag tag);

	/**
	 * Remove the given tag from the list of tags for this branch.
	 * 
	 * @param tag
	 */
	void removeTag(HibTag tag);

	/**
	 * Remove all tags.
	 */
	void removeAllTags();

	/**
	 * Return all tags that were assigned to this branch.
	 *
	 * @return
	 */
	Result<? extends HibTag> getTags();

	/**
	 * Return a page of all visible tags that are assigned to the branch.
	 * 
	 * @param user
	 * @param params
	 * @return Page which contains the result
	 */
	TransformablePage<? extends HibTag> getTags(HibUser user, PagingParameters params);

	/**
	 * Tests if the branch is tagged with the given tag.
	 *
	 * @param tag
	 * @return
	 */
	boolean hasTag(HibTag tag);

	/**
	 * Handle the update tags request.
	 *
	 * @param ac
	 * @param batch
	 * @return Page which includes the new set of tags
	 */
	TransformablePage<? extends HibTag> updateTags(InternalActionContext ac, EventQueueBatch batch);

	/**
	 * Generate event which is send when the branch is set to be the latest of the project.
	 *
	 * @return
	 */
	ProjectBranchEventModel onSetLatest();

	/**
	 * Generate a tagging event for the branch.
	 *
	 * @param tag
	 * @param assignment
	 * @return
	 */
	BranchTaggedEventModel onTagged(HibTag tag, Assignment assignment);

	/**
	 * Create a project schema assignment event.
	 *
	 * @param schemaVersion
	 * @param assigned
	 * @param status
	 * @return
	 */
	BranchSchemaAssignEventModel onSchemaAssignEvent(HibSchemaVersion schemaVersion, Assignment assigned, JobStatus status);

	/**
	 * Create a project microschema assignment event.
	 *
	 * @param microschemaVersion
	 * @param assigned
	 * @param status
	 * @return
	 */
	BranchMicroschemaAssignModel onMicroschemaAssignEvent(HibMicroschemaVersion microschemaVersion, Assignment assigned, JobStatus status);

	/**
	 * Load the tag with the given uuid that was used to tag the branch.
	 *
	 * @param uuid
	 * @return
	 */
	HibTag findTagByUuid(String uuid);

	BranchReference transformToReference();

}
