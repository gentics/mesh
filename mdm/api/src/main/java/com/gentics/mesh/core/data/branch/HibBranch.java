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
import com.gentics.mesh.core.data.HibCoreElement;
import com.gentics.mesh.core.data.HibNamedElement;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.HibReferenceableElement;
import com.gentics.mesh.core.data.Taggable;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.schema.HibFieldSchemaElement;
import com.gentics.mesh.core.data.schema.HibFieldSchemaVersionElement;
import com.gentics.mesh.core.data.schema.HibMicroschema;
import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.data.tag.HibTag;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.data.user.HibUserTracking;
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
public interface HibBranch extends HibCoreElement<BranchResponse>, HibReferenceableElement<BranchReference>, HibUserTracking, HibNamedElement, Taggable {

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
	 * Make the branch the initial branch of the project
	 * 
	 * @return
	 */
	HibBranch setInitial();

	/**
	 * Get the next branches.
	 * 
	 * @return next branches
	 */
	List<? extends HibBranch> getNextBranches();

	/**
	 * Set the previous Branch.
	 *
	 * @param branch
	 *            previous Branch
	 * @return Fluent API
	 */
	HibBranch setPreviousBranch(HibBranch branch);

	/**
	 * Get the previous Branch.
	 * 
	 * @return previous Branch
	 */
	HibBranch getPreviousBranch();

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
	Result<? extends HibSchemaVersion> findAllSchemaVersions();

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
	 * Get an iterable over all active microschema container versions. An active version is one which still contains {@link HibNodeFieldContainer}'s or one
	 * which is queued and will soon contain containers due to an executed node migration.
	 *
	 * @return Iterable
	 */
	Iterable<? extends HibMicroschemaVersion> findActiveMicroschemaVersions();

	/**
	 * Assign the branch to a specific project.
	 * 
	 * @param project
	 * @return Fluent API
	 */
	HibBranch setProject(HibProject project);

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
	Page<? extends HibTag> getTags(HibUser user, PagingParameters params);

	/**
	 * Tests if the branch is tagged with the given tag.
	 *
	 * @param tag
	 * @return
	 */
	boolean hasTag(HibTag tag);

	/**
	 * Load the tag with the given uuid that was used to tag the branch.
	 *
	 * @param uuid
	 * @return
	 */
	HibTag findTagByUuid(String uuid);

	/**
	 * Get an iterable of all latest schema container versions.
	 * 
	 * @return Iterable
	 */
	Iterable<? extends HibBranchSchemaVersion> findAllLatestSchemaVersionEdges();

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
			SC extends HibFieldSchemaElement<R, RM, RE, SC, SCV>, 
			SCV extends HibFieldSchemaVersionElement<R, RM, RE, SC, SCV>
	> E onContainerAssignEvent(SCV schemaVersion, Assignment assigned, JobStatus status, Supplier<E> modelSupplier) {
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
		return model;
	}

	/**
	 * Create a project schema assignment event.
	 *
	 * @param schemaVersion
	 * @param assigned
	 * @param status
	 * @return
	 */
	default BranchSchemaAssignEventModel onSchemaAssignEvent(HibSchemaVersion schemaVersion, Assignment assigned, JobStatus status) {  
		return onContainerAssignEvent(schemaVersion, assigned, status, BranchSchemaAssignEventModel::new);
	}

	/**
	 * Create a project microschema assignment event.
	 *
	 * @param microschemaVersion
	 * @param assigned
	 * @param status
	 * @return
	 */
	default BranchMicroschemaAssignModel onMicroschemaAssignEvent(HibMicroschemaVersion microschemaVersion, Assignment assigned,
		JobStatus status) {
		return onContainerAssignEvent(microschemaVersion, assigned, status, BranchMicroschemaAssignModel::new);
	}

	/**
	 * Handle the update tags request.
	 *
	 * @param ac
	 * @param batch
	 * @return Page which includes the new set of tags
	 */
	default Page<? extends HibTag> updateTags(InternalActionContext ac, EventQueueBatch batch) {
		List<HibTag> tags = getTagsToSet(ac, batch);
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
		HibProject project = getProject();
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
	default BranchTaggedEventModel onTagged(HibTag tag, Assignment assignment) {
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
		HibProject project = getProject();
		ProjectReference reference = project.transformToReference();
		model.setProject(reference);

		return model;
	}
}
