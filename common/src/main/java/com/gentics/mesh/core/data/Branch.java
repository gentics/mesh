package com.gentics.mesh.core.data;

import static com.gentics.mesh.ElementType.BRANCH;
import static com.gentics.mesh.core.rest.MeshEvent.BRANCH_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.BRANCH_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.BRANCH_UPDATED;

import java.util.List;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.TypeInfo;
import com.gentics.mesh.core.data.branch.BranchMicroschemaEdge;
import com.gentics.mesh.core.data.branch.BranchSchemaEdge;
import com.gentics.mesh.core.data.job.Job;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.data.root.BranchRoot;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.data.schema.MicroschemaContainerVersion;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.rest.branch.BranchReference;
import com.gentics.mesh.core.rest.branch.BranchResponse;
import com.gentics.mesh.core.rest.event.branch.BranchMicroschemaAssignModel;
import com.gentics.mesh.core.rest.event.branch.BranchSchemaAssignEventModel;
import com.gentics.mesh.core.rest.event.branch.BranchTaggedEventModel;
import com.gentics.mesh.core.rest.event.project.ProjectBranchEventModel;
import com.gentics.mesh.core.rest.job.JobStatus;
import com.gentics.mesh.event.Assignment;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.madlmigration.TraversalResult;
import com.gentics.mesh.parameter.PagingParameters;

/**
 * The Branch domain model interface.
 *
 * A branch is a bundle of specific schema versions which are used within a project. Branches can be used to create multiple tree structures within a single
 * project.
 * 
 * The branch will keep track of assigned versions and also store the information which schema version has ever been assigned to the branch.
 * 
 * A branch has the following responsibilities:
 * 
 * <ul>
 * <li>Manage assigned branches for the REST API</li>
 * <li>Provide information for node migration handlers. A handler must know what version needs to be migrated.</li>
 * <li>Provide information to the search index handler so that a list of indices can be compiled which should be used when searching</li>
 * <ul>
 * 
 * The latest version will be used for the creation of new nodes and should never be be downgraded. The other assigned versions will be used to manage
 * migrations and identify which branch specific search indices should be used when using the search indices.
 * 
 */
public interface Branch
	extends MeshCoreVertex<BranchResponse, Branch>, NamedElement, ReferenceableElement<BranchReference>, UserTrackingVertex, Taggable, ProjectElement {

	TypeInfo TYPE_INFO = new TypeInfo(BRANCH, BRANCH_CREATED, BRANCH_UPDATED, BRANCH_DELETED);

	@Override
	default TypeInfo getTypeInfo() {
		return TYPE_INFO;
	}

	static final String NAME = "name";

	static final String HOSTNAME = "hostname";

	static final String SSL = "ssl";

	static final String PATH_PREFIX = "pathPrefix";

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
	Branch setActive(boolean active);

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
	Branch setMigrated(boolean migrated);

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
	Branch setHostname(String hostname);

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
	Branch setSsl(boolean ssl);

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
	Branch setPathPrefix(String pathPrefix);

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
	Branch setLatest();

	/**
	 * Get the next Branch.
	 * 
	 * @return next Branch
	 */
	Branch getNextBranch();

	/**
	 * Set the next Branch.
	 * 
	 * @param branch
	 *            next Branch
	 * @return Fluent API
	 */
	Branch setNextBranch(Branch branch);

	/**
	 * Get the previous Branch.
	 * 
	 * @return previous Branch
	 */
	Branch getPreviousBranch();

	/**
	 * Get the root vertex.
	 * 
	 * @return branch root to which the branch belongs
	 */
	BranchRoot getRoot();

	/**
	 * Assign the given schema version to the branch and queue a job which will trigger the migration.
	 * 
	 * @param user
	 * @param schemaContainerVersion
	 * @param batch
	 * @return Job which was created to trigger the migration or null if no job was created because the version has already been assigned before
	 */
	Job assignSchemaVersion(User user, SchemaContainerVersion schemaContainerVersion, EventQueueBatch batch);

	/**
	 * Unassign all schema versions of the given schema from this branch.
	 * 
	 * @param schemaContainer
	 * @return Fluent API
	 */
	Branch unassignSchema(SchemaContainer schemaContainer);

	/**
	 * Check whether a version of this schema container is assigned to this branch.
	 *
	 * @param schema
	 *            schema
	 * @return true iff assigned
	 */
	boolean contains(SchemaContainer schema);

	/**
	 * Check whether the given schema container version is assigned to this branch.
	 *
	 * @param schemaContainerVersion
	 *            schema container version
	 * @return true if assigned
	 */
	boolean contains(SchemaContainerVersion schemaContainerVersion);

	/**
	 * Get an iterable of all schema container versions.
	 * 
	 * @return Iterable
	 */
	Iterable<? extends SchemaContainerVersion> findAllSchemaVersions();

	/**
	 * Assign the given microschema version to the branch and queue a job which executes the migration.
	 * 
	 * @param user
	 * 
	 * @param microschemaContainerVersion
	 * @param batch
	 * @return Job which has been created if the version has not yet been assigned. Otherwise null will be returned.
	 */
	Job assignMicroschemaVersion(User user, MicroschemaContainerVersion microschemaContainerVersion, EventQueueBatch batch);

	/**
	 * Unassigns all versions of the given microschema from this branch.
	 * 
	 * @param microschemaContainer
	 * @return Fluent API
	 */
	Branch unassignMicroschema(MicroschemaContainer microschemaContainer);

	/**
	 * Check whether a version of this microschema container is assigned to this branch.
	 *
	 * @param microschema
	 *            microschema
	 * @return true iff assigned
	 */
	boolean contains(MicroschemaContainer microschema);

	/**
	 * Check whether the given microschema container version is assigned to this branch.
	 *
	 * @param microschemaContainerVersion
	 *            microschema container version
	 * @return true iff assigned
	 */
	boolean contains(MicroschemaContainerVersion microschemaContainerVersion);

	/**
	 * Get an iterable of all microschema container versions.
	 * 
	 * @return Iterable
	 */
	Iterable<? extends MicroschemaContainerVersion> findAllMicroschemaVersions();

	/**
	 * Get an iterable of all latest microschema container versions.
	 * 
	 * @return Iterable
	 */
	Iterable<? extends BranchMicroschemaEdge> findAllLatestMicroschemaVersionEdges();

	/**
	 * Get an iterable over all active schema container versions. An active version is one which still contains {@link NodeGraphFieldContainer}'s or one which
	 * is queued and will soon contain containers due to an executed node migration.
	 * 
	 * @return Iterable
	 */
	TraversalResult<? extends SchemaContainerVersion> findActiveSchemaVersions();

	/**
	 * Get an iterable over all active microschema container versions. An active version is one which still contains {@link NodeGraphFieldContainer}'s or one
	 * which is queued and will soon contain containers due to an executed node migration.
	 *
	 * @return Iterable
	 */
	Iterable<? extends MicroschemaContainerVersion> findActiveMicroschemaVersions();

	/**
	 * Get an iterable of all latest schema container versions.
	 * 
	 * @return Iterable
	 */
	Iterable<? extends BranchSchemaEdge> findAllLatestSchemaVersionEdges();

	/**
	 * Assign the branch to a specific project.
	 * 
	 * @param project
	 * @return Fluent API
	 */
	Branch setProject(Project project);

	/**
	 * Return all schema versions which are linked to the branch.
	 * 
	 * @return
	 */
	Iterable<? extends BranchSchemaEdge> findAllSchemaVersionEdges();

	/**
	 * Return all microschema versions which are linked to the branch.
	 * 
	 * @return
	 */
	Iterable<? extends BranchMicroschemaEdge> findAllMicroschemaVersionEdges();

	/**
	 * Find the branch schema edge for the given version.
	 * 
	 * @param schemaContainerVersion
	 * @return Found edge between branch and version
	 */
	BranchSchemaEdge findBranchSchemaEdge(SchemaContainerVersion schemaContainerVersion);

	/**
	 * Find the branch microschema edge for the given version.
	 * 
	 * @param microschemaContainerVersion
	 * @return Found edge between branch and version
	 */
	BranchMicroschemaEdge findBranchMicroschemaEdge(MicroschemaContainerVersion microschemaContainerVersion);

	/**
	 * Find the latest schema version which is assigned to the branch which matches the provided schema container
	 * 
	 * @param schemaContainer
	 * @return Found version or null if no version could be found.
	 */
	SchemaContainerVersion findLatestSchemaVersion(SchemaContainer schemaContainer);

	/**
	 * Find the latest microschema version which is assigned to the branch which matches the provided microschema container
	 * 
	 * @param schemaContainer
	 * @return Found version or null if no version could be found.
	 */
	MicroschemaContainerVersion findLatestMicroschemaVersion(MicroschemaContainer schemaContainer);

	/**
	 * Add the given tag to the list of tags for this branch.
	 * 
	 * @param tag
	 */
	void addTag(Tag tag);

	/**
	 * Remove the given tag from the list of tags for this branch.
	 * 
	 * @param tag
	 */
	void removeTag(Tag tag);

	/**
	 * Remove all tags.
	 */
	void removeAllTags();

	/**
	 * Return a list of all tags that were assigned to this branch.
	 *
	 * @return
	 */
	List<? extends Tag> getTags();

	/**
	 * Return a page of all visible tags that are assigned to the branch.
	 * 
	 * @param user
	 * @param params
	 * @return Page which contains the result
	 */
	TransformablePage<? extends Tag> getTags(User user, PagingParameters params);

	/**
	 * Tests if the branch is tagged with the given tag.
	 *
	 * @param tag
	 * @return
	 */
	boolean hasTag(Tag tag);

	/**
	 * Handle the update tags request.
	 *
	 * @param ac
	 * @param batch
	 * @return Page which includes the new set of tags
	 */
	TransformablePage<? extends Tag> updateTags(InternalActionContext ac, EventQueueBatch batch);

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
	BranchTaggedEventModel onTagged(Tag tag, Assignment assignment);

	/**
	 * Create a project schema assignment event.
	 *
	 * @param schemaContainerVersion
	 * @param assigned
	 * @param status
	 * @return
	 */
	BranchSchemaAssignEventModel onSchemaAssignEvent(SchemaContainerVersion schemaContainerVersion, Assignment assigned, JobStatus status);

	/**
	 * Create a project microschema assignment event.
	 *
	 * @param microschemaContainerVersion
	 * @param assigned
	 * @param status
	 * @return
	 */
	BranchMicroschemaAssignModel onMicroschemaAssignEvent(MicroschemaContainerVersion microschemaContainerVersion, Assignment assigned, JobStatus status);

}
