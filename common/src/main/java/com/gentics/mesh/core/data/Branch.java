package com.gentics.mesh.core.data;

import static com.gentics.mesh.Events.EVENT_RELEASE_CREATED;
import static com.gentics.mesh.Events.EVENT_RELEASE_DELETED;
import static com.gentics.mesh.Events.EVENT_RELEASE_UPDATED;

import com.gentics.mesh.core.TypeInfo;
import com.gentics.mesh.core.data.branch.BranchMicroschemaEdge;
import com.gentics.mesh.core.data.branch.BranchSchemaEdge;
import com.gentics.mesh.core.data.job.Job;
import com.gentics.mesh.core.data.root.BranchRoot;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.data.schema.MicroschemaContainerVersion;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.rest.branch.BranchReference;
import com.gentics.mesh.core.rest.branch.BranchResponse;

/**
 * The Release domain model interface.
 *
 * A release is a bundle of specific schema versions which are used within a project. Releases can be used to create multiple tree structures within a single
 * project.
 * 
 * The release will keep track of assigned versions and also store the information which schema version has ever been assigned to the release.
 * 
 * A release has the following responsibilities:
 * 
 * <ul>
 * <li>Manage assigned releases for the REST API</li>
 * <li>Provide information for node migration handlers. A handler must know what version needs to be migrated.</li>
 * <li>Provide information to the search index handler so that a list of indices can be compiled which should be used when searching</li>
 * <ul>
 * 
 * The latest version will be used for the creation of new nodes and should never be be downgraded. The other assigned versions will be used to manage
 * migrations and identify which release specific search indices should be used when using the search indices.
 * 
 */
public interface Branch extends MeshCoreVertex<BranchResponse, Branch>, NamedElement, ReferenceableElement<BranchReference>, UserTrackingVertex {

	/**
	 * Type Value: {@value #TYPE}
	 */
	String TYPE = "release";

	TypeInfo TYPE_INFO = new TypeInfo(TYPE, EVENT_RELEASE_CREATED, EVENT_RELEASE_UPDATED, EVENT_RELEASE_DELETED);

	@Override
	default TypeInfo getTypeInfo() {
		return TYPE_INFO;
	}

	static final String NAME = "name";

	static final String HOSTNAME = "hostname";

	static final String SSL = "ssl";

	/**
	 * Get whether the release is active.
	 * 
	 * @return true for active release
	 */
	boolean isActive();

	/**
	 * Set whether the release is active.
	 * 
	 * @param active
	 *            true for active
	 * @return Fluent API
	 */
	Branch setActive(boolean active);

	/**
	 * Get whether all nodes of the previous release have been migrated.
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
	 * Return the configured hostname of the release.
	 * 
	 * @return
	 */
	String getHostname();

	/**
	 * Set the hostname of the release.
	 * 
	 * @param hostname
	 * @return
	 */
	Branch setHostname(String hostname);

	/**
	 * Return the ssl flag of the release.
	 * 
	 * @return
	 */
	Boolean getSsl();

	/**
	 * Set the ssl flag of the release.
	 * 
	 * @param ssl
	 * @return
	 */
	Branch setSsl(boolean ssl);

	/**
	 * Get the next Release.
	 * 
	 * @return next Release
	 */
	Branch getNextBranch();

	/**
	 * Set the next Release.
	 * 
	 * @param release
	 *            next Release
	 * @return Fluent API
	 */
	Branch setNextBranch(Branch release);

	/**
	 * Get the previous Release.
	 * 
	 * @return previous Release
	 */
	Branch getPreviousBranch();

	/**
	 * Get the root vertex.
	 * 
	 * @return release root to which the release belongs
	 */
	BranchRoot getRoot();

	/**
	 * Assign the given schema version to the release and queue a job which will trigger the migration.
	 * 
	 * @param user
	 * @param schemaContainerVersion
	 * @return Job which was created to trigger the migration or null if no job was created because the version has already been assigned before
	 */
	Job assignSchemaVersion(User user, SchemaContainerVersion schemaContainerVersion);

	/**
	 * Unassign all schema versions of the given schema from this release.
	 * 
	 * @param schemaContainer
	 * @return Fluent API
	 */
	Branch unassignSchema(SchemaContainer schemaContainer);

	/**
	 * Check whether a version of this schema container is assigned to this release.
	 *
	 * @param schema
	 *            schema
	 * @return true iff assigned
	 */
	boolean contains(SchemaContainer schema);

	/**
	 * Check whether the given schema container version is assigned to this release.
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
	 * Assign the given microschema version to the release and queue a job which executes the migration.
	 * 
	 * @param user
	 * 
	 * @param microschemaContainerVersion
	 * @return Job which has been created if the version has not yet been assigned. Otherwise null will be returned.
	 */
	Job assignMicroschemaVersion(User user, MicroschemaContainerVersion microschemaContainerVersion);

	/**
	 * Unassigns all versions of the given microschema from this release.
	 * 
	 * @param microschemaContainer
	 * @return Fluent API
	 */
	Branch unassignMicroschema(MicroschemaContainer microschemaContainer);

	/**
	 * Check whether a version of this microschema container is assigned to this release.
	 *
	 * @param microschema
	 *            microschema
	 * @return true iff assigned
	 */
	boolean contains(MicroschemaContainer microschema);

	/**
	 * Check whether the given microschema container version is assigned to this release.
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
	Iterable<? extends SchemaContainerVersion> findActiveSchemaVersions();

	/**
	 * Get an iterable of all latest schema container versions.
	 * 
	 * @return Iterable
	 */
	Iterable<? extends BranchSchemaEdge> findAllLatestSchemaVersionEdges();

	/**
	 * Project to which the release belongs.
	 * 
	 * @return Project of the release
	 */
	Project getProject();

	/**
	 * Assign the release to a specific project.
	 * 
	 * @param project
	 * @return Fluent API
	 */
	Branch setProject(Project project);

	/**
	 * Return all schema versions which are linked to the release.
	 * 
	 * @return
	 */
	Iterable<? extends BranchSchemaEdge> findAllSchemaVersionEdges();

	/**
	 * Return all microschema versions which are linked to the release.
	 * 
	 * @return
	 */
	Iterable<? extends BranchMicroschemaEdge> findAllMicroschemaVersionEdges();

	/**
	 * Find the release schema edge for the given version.
	 * 
	 * @param schemaContainerVersion
	 * @return Found edge between release and version
	 */
	BranchSchemaEdge findReleaseSchemaEdge(SchemaContainerVersion schemaContainerVersion);

	/**
	 * Find the release microschema edge for the given version.
	 * 
	 * @param microschemaContainerVersion
	 * @return Found edge between release and version
	 */
	BranchMicroschemaEdge findReleaseMicroschemaEdge(MicroschemaContainerVersion microschemaContainerVersion);

	/**
	 * Find the latest schema version which is assigned to the release which matches the provided schema container
	 * 
	 * @param schemaContainer
	 * @return Found version or null if no version could be found.
	 */
	SchemaContainerVersion findLatestSchemaVersion(SchemaContainer schemaContainer);

	/**
	 * Find the latest microschema version which is assigned to the release which matches the provided microschema container
	 * 
	 * @param schemaContainer
	 * @return Found version or null if no version could be found.
	 */
	MicroschemaContainerVersion findLatestMicroschemaVersion(MicroschemaContainer schemaContainer);

}
