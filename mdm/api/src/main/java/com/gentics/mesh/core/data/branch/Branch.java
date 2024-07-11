package com.gentics.mesh.core.data.branch;

import static com.gentics.mesh.ElementType.BRANCH;
import static com.gentics.mesh.core.rest.MeshEvent.BRANCH_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.BRANCH_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.BRANCH_TAGGED;
import static com.gentics.mesh.core.rest.MeshEvent.BRANCH_UNTAGGED;
import static com.gentics.mesh.core.rest.MeshEvent.BRANCH_UPDATED;
import static com.gentics.mesh.core.rest.MeshEvent.PROJECT_LATEST_BRANCH_UPDATED;
import static com.gentics.mesh.event.Assignment.ASSIGNED;
import static com.gentics.mesh.util.URIUtils.encodeSegment;

import java.util.List;
import java.util.function.Supplier;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.TypeInfo;
import com.gentics.mesh.core.data.CoreElement;
import com.gentics.mesh.core.data.NamedBaseElement;
import com.gentics.mesh.core.data.NodeFieldContainer;
import com.gentics.mesh.core.data.ProjectElement;
import com.gentics.mesh.core.data.ReferenceableElement;
import com.gentics.mesh.core.data.Taggable;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.project.Project;
import com.gentics.mesh.core.data.schema.FieldSchemaElement;
import com.gentics.mesh.core.data.schema.FieldSchemaVersionElement;
import com.gentics.mesh.core.data.schema.Microschema;
import com.gentics.mesh.core.data.schema.MicroschemaVersion;
import com.gentics.mesh.core.data.schema.Schema;
import com.gentics.mesh.core.data.schema.SchemaVersion;
import com.gentics.mesh.core.data.tag.Tag;
import com.gentics.mesh.core.data.user.User;
import com.gentics.mesh.core.data.user.UserTracking;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.branch.BranchReference;
import com.gentics.mesh.core.rest.branch.BranchResponse;
import com.gentics.mesh.core.rest.common.NameUuidReference;
import com.gentics.mesh.core.rest.event.branch.AbstractBranchAssignEventModel;
import com.gentics.mesh.core.rest.event.branch.BranchMeshEventModel;
import com.gentics.mesh.core.rest.event.branch.BranchMicroschemaAssignModel;
import com.gentics.mesh.core.rest.event.branch.BranchSchemaAssignEventModel;
import com.gentics.mesh.core.rest.event.branch.BranchTaggedEventModel;
import com.gentics.mesh.core.rest.event.project.ProjectBranchEventModel;
import com.gentics.mesh.core.rest.job.JobStatus;
import com.gentics.mesh.core.rest.project.ProjectReference;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainerVersion;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.event.Assignment;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.handler.VersionUtils;
import com.gentics.mesh.parameter.PagingParameters;

/**
 * Domain model for branch.
 */
public interface Branch extends CoreElement<BranchResponse>, ReferenceableElement<BranchReference>, 
		UserTracking, ProjectElement, NamedBaseElement, Taggable {

	TypeInfo TYPE_INFO = new TypeInfo(BRANCH, BRANCH_CREATED, BRANCH_UPDATED, BRANCH_DELETED);

	@Override
	default TypeInfo getTypeInfo() {
		return TYPE_INFO;
	}

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
	 * Make the branch the initial branch of the project
	 * 
	 * @return
	 */
	Branch setInitial();

	/**
	 * Get the next branches.
	 * 
	 * @return next branches
	 */
	List<? extends Branch> getNextBranches();

	/**
	 * Set the previous Branch.
	 *
	 * @param branch
	 *            previous Branch
	 * @return Fluent API
	 */
	Branch setPreviousBranch(Branch branch);

	/**
	 * Get the previous Branch.
	 * 
	 * @return previous Branch
	 */
	Branch getPreviousBranch();

	/**
	 * Unassign all schema versions of the given schema from this branch.
	 * 
	 * @param schemaContainer
	 * @return Fluent API
	 */
	Branch unassignSchema(Schema schemaContainer);

	/**
	 * Check whether a version of this schema container is assigned to this branch.
	 *
	 * @param schema
	 *            schema
	 * @return true iff assigned
	 */
	boolean contains(Schema schema);

	/**
	 * Check whether the given schema container version is assigned to this branch.
	 *
	 * @param schemaVersion
	 *            schema container version
	 * @return true if assigned
	 */
	boolean contains(SchemaVersion schemaVersion);

	/**
	 * Get an traversal result of all schema container versions.
	 * 
	 * @return
	 */
	Result<? extends SchemaVersion> findAllSchemaVersions();

	/**
	 * Unassigns all versions of the given microschema from this branch.
	 * 
	 * @param microschema
	 * @return Fluent API
	 */
	Branch unassignMicroschema(Microschema microschema);

	/**
	 * Check whether a version of this microschema container is assigned to this branch.
	 *
	 * @param microschema
	 *            microschema
	 * @return true iff assigned
	 */
	boolean contains(Microschema microschema);

	/**
	 * Check whether the given microschema container version is assigned to this branch.
	 *
	 * @param microschemaVersion
	 *            microschema container version
	 * @return true iff assigned
	 */
	boolean contains(MicroschemaVersion microschemaVersion);

	/**
	 * Get an iterable of all microschema container versions.
	 * 
	 * @return Iterable
	 */
	Result<? extends MicroschemaVersion> findAllMicroschemaVersions();

	/**
	 * Get an iterable of all latest microschema container versions.
	 * 
	 * @return Iterable
	 */
	Result<? extends BranchMicroschemaVersion> findAllLatestMicroschemaVersionEdges();

	/**
	 * Get an iterable over all active schema container versions. An active version is one which still contains {@link NodeGraphFieldContainer}'s or one which
	 * is queued and will soon contain containers due to an executed node migration.
	 * 
	 * @return Iterable
	 */
	Result<? extends SchemaVersion> findActiveSchemaVersions();

	/**
	 * Get an iterable over all active microschema container versions. An active version is one which still contains {@link NodeFieldContainer}'s or one
	 * which is queued and will soon contain containers due to an executed node migration.
	 *
	 * @return Iterable
	 */
	Iterable<? extends MicroschemaVersion> findActiveMicroschemaVersions();

	/**
	 * Assign the branch to a specific project.
	 * 
	 * @param project
	 * @return Fluent API
	 */
	Branch setProject(Project project);

	/**
	 * Find the latest schema version which is assigned to the branch which matches the provided schema container
	 * 
	 * @param schemaContainer
	 * @return Found version or null if no version could be found.
	 */
	SchemaVersion findLatestSchemaVersion(Schema schemaContainer);

	/**
	 * Find the latest microschema version which is assigned to the branch which matches the provided microschema container
	 * 
	 * @param schemaContainer
	 * @return Found version or null if no version could be found.
	 */
	MicroschemaVersion findLatestMicroschemaVersion(Microschema schemaContainer);

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
	 * Return all tags that were assigned to this branch.
	 *
	 * @return
	 */
	Result<? extends Tag> getTags();

	/**
	 * Return a page of all visible tags that are assigned to the branch.
	 * 
	 * @param user
	 * @param params
	 * @return Page which contains the result
	 */
	Page<? extends Tag> getTags(User user, PagingParameters params);

	/**
	 * Tests if the branch is tagged with the given tag.
	 *
	 * @param tag
	 * @return
	 */
	boolean hasTag(Tag tag);

	/**
	 * Load the tag with the given uuid that was used to tag the branch.
	 *
	 * @param uuid
	 * @return
	 */
	Tag findTagByUuid(String uuid);

	/**
	 * Get an iterable of all latest schema container versions.
	 * 
	 * @return Iterable
	 */
	Iterable<? extends BranchSchemaVersion> findAllLatestSchemaVersionEdges();

	/**
	 * Return all schema versions which are linked to the branch.
	 * 
	 * @return
	 */
	Result<? extends BranchSchemaVersion> findAllSchemaVersionEdges();

	/**
	 * Return all microschema versions which are linked to the branch.
	 * 
	 * @return
	 */
	Result<? extends BranchMicroschemaVersion> findAllMicroschemaVersionEdges();

	@Override
	default String getAPIPath(InternalActionContext ac) {
		return VersionUtils.baseRoute(ac) + "/" + encodeSegment(getProject().getName()) + "/branches/" + getUuid();
	}

	@Override
	default String getSubETag(InternalActionContext ac) {
		return String.valueOf(getLastEditedTimestamp());
	}

	/**
	 * Create a generic container assignment event.
	 * 
	 * @param <E> branch assignment model type
	 * @param <R> REST model type
	 * @param <RM> version model type
	 * @param <RE> schema model reference type
	 * @param <SC> entity type
	 * @param <SCV> entity version model type
	 * @param schemaVersion
	 * @param assigned
	 * @param status
	 * @param modelSupplier
	 * @return
	 */
	default <
			E extends AbstractBranchAssignEventModel<RE>,
			R extends FieldSchemaContainer, 
			RM extends FieldSchemaContainerVersion, 
			RE extends NameUuidReference<RE>, 
			SC extends FieldSchemaElement<R, RM, RE, SC, SCV>, 
			SCV extends FieldSchemaVersionElement<R, RM, RE, SC, SCV>
	> E onContainerAssignEvent(SCV schemaVersion, Assignment assigned, JobStatus status, SCV oldVersion, Supplier<E> modelSupplier) {
		E model = modelSupplier.get();
		model.setOrigin(Tx.get().data().options().getNodeName());
		switch (assigned) {
		case ASSIGNED:
			model.setEvent(schemaVersion.getBranchAssignEvent());
			break;
		case UNASSIGNED:
			model.setEvent(schemaVersion.getBranchUnassignEvent());
			break;
		}
		model.setSchema(schemaVersion.transformToReference());
		model.setStatus(status);
		model.setBranch(transformToReference());
		model.setProject(getProject().transformToReference());
		if (oldVersion != null) {
			model.setOldSchema(oldVersion.transformToReference());
		}
		return model;
	}

	/**
	 * Create a project schema assignment event.
	 *
	 * @param schemaVersion
	 * @param assigned
	 * @param status
	 * @param oldVersion old version (may be null)
	 * @return
	 */
	default BranchSchemaAssignEventModel onSchemaAssignEvent(SchemaVersion schemaVersion, Assignment assigned, JobStatus status, SchemaVersion oldVersion) {  
		return onContainerAssignEvent(schemaVersion, assigned, status, oldVersion, BranchSchemaAssignEventModel::new);
	}

	/**
	 * Create a project microschema assignment event.
	 *
	 * @param microschemaVersion
	 * @param assigned
	 * @param status
	 * @param oldVersion old version (may be null)
	 * @return
	 */
	default BranchMicroschemaAssignModel onMicroschemaAssignEvent(MicroschemaVersion microschemaVersion, Assignment assigned,
		JobStatus status, MicroschemaVersion oldVersion) {
		return onContainerAssignEvent(microschemaVersion, assigned, status, oldVersion, BranchMicroschemaAssignModel::new);
	}

	/**
	 * Handle the update tags request.
	 *
	 * @param ac
	 * @param batch
	 * @return Page which includes the new set of tags
	 */
	default Page<? extends Tag> updateTags(InternalActionContext ac, EventQueueBatch batch) {
		List<Tag> tags = getTagsToSet(ac, batch);
		// TODO Rework this code. We should only add the needed tags and don't dispatch all events.
		removeAllTags();
		tags.forEach(tag -> {
			batch.add(onTagged(tag, ASSIGNED));
			addTag(tag);
		});
		return getTags(ac.getUser(), ac.getPagingParameters());
	}

	/**
	 * Generate event which is send when the branch is set to be the latest of the project.
	 *
	 * @return
	 */
	default ProjectBranchEventModel onSetLatest() {
		ProjectBranchEventModel model = new ProjectBranchEventModel();
		model.setEvent(PROJECT_LATEST_BRANCH_UPDATED);

		// .project
		Project project = getProject();
		ProjectReference reference = project.transformToReference();
		model.setProject(reference);

		fillEventInfo(model);
		return model;
	}

	/**
	 * Generate a tagging event for the branch.
	 *
	 * @param tag
	 * @param assignment
	 * @return
	 */
	default BranchTaggedEventModel onTagged(Tag tag, Assignment assignment) {
		BranchTaggedEventModel model = new BranchTaggedEventModel();
		model.setTag(tag.transformToReference());
		model.setBranch(transformToReference());
		model.setProject(getProject().transformToReference());
		switch (assignment) {
		case ASSIGNED:
			model.setEvent(BRANCH_TAGGED);
			break;
		case UNASSIGNED:
			model.setEvent(BRANCH_UNTAGGED);
			break;
		}

		return model;
	}

	@Override
	default BranchMeshEventModel onCreated() {
		return createEvent(getTypeInfo().getOnCreated());
	}

	@Override
	default BranchMeshEventModel createEvent(MeshEvent event) {
		BranchMeshEventModel model = new BranchMeshEventModel();
		model.setEvent(event);
		fillEventInfo(model);

		// .project
		Project project = getProject();
		ProjectReference reference = project.transformToReference();
		model.setProject(reference);

		return model;
	}
}
