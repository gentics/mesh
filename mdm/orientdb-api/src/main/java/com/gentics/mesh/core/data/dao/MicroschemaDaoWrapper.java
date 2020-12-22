package com.gentics.mesh.core.data.dao;

import java.util.Map;
import java.util.function.Predicate;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.schema.HibMicroschema;
import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;
import com.gentics.mesh.core.data.schema.Microschema;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.rest.microschema.MicroschemaVersionModel;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaResponse;
import com.gentics.mesh.core.rest.schema.MicroschemaModel;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangesListModel;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.parameter.PagingParameters;

public interface MicroschemaDaoWrapper extends MicroschemaDao, DaoWrapper<HibMicroschema>, DaoTransformable<HibMicroschema, MicroschemaResponse> {

	/**
	 * Load the microschema by uuid.
	 * 
	 * @param ac
	 * @param schemaUuid
	 * @param perm
	 * @return
	 */
	HibMicroschema loadObjectByUuid(InternalActionContext ac, String schemaUuid, InternalPermission perm);

	/**
	 * Find microschema by uuid.
	 * 
	 * @param uuid
	 * @return
	 */
	HibMicroschema findByUuid(String uuid);

	// boolean update(MicroschemaContainer microschema, InternalActionContext ac,
	// EventQueueBatch batch);

	/**
	 * Create a new microschema container.
	 * 
	 * @param microschema
	 * @param user
	 *            User that is used to set creator and editor references.
	 * @param batch
	 * @return
	 */
	default HibMicroschema create(MicroschemaVersionModel microschema, HibUser user, EventQueueBatch batch) {
		return create(microschema, user, null, batch);
	}

	/**
	 * Create a new microschema.
	 * 
	 * @param microschema
	 * @param user
	 *            User that is used to set creator and editor references.
	 * @param uuid
	 *            optional uuid
	 * @param batch
	 * @return
	 */
	HibMicroschema create(MicroschemaVersionModel microschema, HibUser user, String uuid, EventQueueBatch batch);

	/**
	 * Create a new microschema.
	 * 
	 * @param ac
	 * @param batch
	 * @param uuid
	 * @return
	 */
	HibMicroschema create(InternalActionContext ac, EventQueueBatch batch, String uuid);

	/**
	 * Load a page of microschemas.
	 * 
	 * @param ac
	 * @param pagingInfo
	 * @return
	 */
	Page<? extends Microschema> findAll(InternalActionContext ac, PagingParameters pagingInfo);

	/**
	 * Load a page of microschemas.
	 * 
	 * @param ac
	 * @param pagingInfo
	 * @param extraFilter
	 * @return
	 */
	Page<? extends Microschema> findAll(InternalActionContext ac, PagingParameters pagingInfo,
		Predicate<Microschema> extraFilter);

	/**
	 * Load the microschema by uuid.
	 * 
	 * @param ac
	 * @param uuid
	 * @param perm
	 * @param errorIfNotFound
	 * @return
	 */
	HibMicroschema loadObjectByUuid(InternalActionContext ac, String uuid, InternalPermission perm,
		boolean errorIfNotFound);

	/**
	 * Find the microschema by name.
	 * 
	 * @param name
	 * @return
	 */
	HibMicroschema findByName(String name);

	/**
	 * Return all microschemas.
	 * 
	 * @return
	 */
	Result<? extends Microschema> findAll();

	/**
	 * Load the microschema version via the given reference.
	 * 
	 * @param reference
	 * @return
	 */
	default HibMicroschemaVersion fromReference(MicroschemaReference reference) {
		return fromReference(null, reference);
	}

	/**
	 * Get the microschema container version from the given reference.
	 * 
	 * @param reference
	 *            reference
	 * @return
	 */
	default HibMicroschemaVersion fromReference(HibProject project, MicroschemaReference reference) {
		return fromReference(project, reference, null);
	}

	/**
	 * Get the microschema container version from the given reference. Ignore the version number from the reference, but take the version from the branch
	 * instead.
	 * 
	 * @param project
	 * @param reference
	 *            reference
	 * @param branch
	 *            branch
	 * @return
	 */
	HibMicroschemaVersion fromReference(HibProject project, MicroschemaReference reference, HibBranch branch);

	/**
	 * Delete the microschema.
	 * 
	 * @param microschema
	 * @param bac
	 */
	void delete(HibMicroschema microschema, BulkActionContext bac);

	/**
	 * Check whether the microschema is linked to the project.
	 * 
	 * @param microschema
	 * @param project
	 * @return
	 */
	boolean isLinkedToProject(HibMicroschema microschema, HibProject project);

	/**
	 * Apply changes to the microschema version.
	 * 
	 * @param version
	 * @param ac
	 *            Action context which contains the changes payload
	 * @param batch
	 * @return
	 */
	HibMicroschemaVersion applyChanges(HibMicroschemaVersion version, InternalActionContext ac, EventQueueBatch batch);

	/**
	 * Apply changes to the microschema.
	 * 
	 * @param version
	 * @param ac
	 * @param model
	 * @param batch
	 * @return
	 */
	HibMicroschemaVersion applyChanges(HibMicroschemaVersion version, InternalActionContext ac,
		SchemaChangesListModel model, EventQueueBatch batch);

	/**
	 * Diff the microschema version with the given microschema REST model.
	 * 
	 * @param version
	 * @param ac
	 * @param requestModel
	 * @return List of detected schema changes
	 */
	SchemaChangesListModel diff(HibMicroschemaVersion version, InternalActionContext ac, MicroschemaModel requestModel);

	/**
	 * Find all versions for the given microschema.
	 * 
	 * @param microschema
	 * @return
	 */
	Iterable<? extends HibMicroschemaVersion> findAllVersions(HibMicroschema microschema);

	/**
	 * Find all branches which reference the microschema.
	 * 
	 * @param microschema
	 * @return
	 */
	Map<HibBranch, HibMicroschemaVersion> findReferencedBranches(HibMicroschema microschema);

	/**
	 * Return all draft contents which reference the microschema version.
	 * 
	 * @param version
	 * @param branchUuid
	 * @return
	 */
	Result<? extends NodeGraphFieldContainer> findDraftFieldContainers(HibMicroschemaVersion version,
		String branchUuid);

	/**
	 * Unlink the microschema from the project.
	 * 
	 * @param microschema
	 * @param project
	 * @param batch
	 */
	void unlink(HibMicroschema microschema, HibProject project, EventQueueBatch batch);

	/**
	 * Transform the microschema to a REST response
	 * 
	 * @param microschema
	 * @param ac
	 * @param level
	 * @param languageTags
	 * @return
	 */
	MicroschemaResponse transformToRestSync(HibMicroschema microschema, InternalActionContext ac, int level,
		String... languageTags);

	/**
	 * Return the etag of the microschema.
	 * 
	 * @param schema
	 * @param ac
	 * @return
	 */
	String getETag(HibMicroschema schema, InternalActionContext ac);

	/**
	 * Add the microschema to the database.
	 * 
	 * @param schema
	 * @param user
	 * @param batch
	 */
	void addMicroschema(HibMicroschema schema, HibUser user, EventQueueBatch batch);

	/**
	 * Find all microschemas which are linked to the project.
	 * 
	 * @param project
	 * @return
	 */
	Result<? extends HibMicroschema> findAll(HibProject project);

	/**
	 * Find all microschema versions for the given branch.
	 * 
	 * @param branch
	 * @return
	 */
	Result<HibMicroschemaVersion> findActiveMicroschemaVersions(HibBranch branch);

	/**
	 * Load a page of microschemas.
	 * 
	 * @param project
	 * @param ac
	 * @param pagingInfo
	 * @return
	 */
	Page<? extends HibMicroschema> findAll(HibProject project, InternalActionContext ac,
		PagingParameters pagingInfo);

	/**
	 * Find the microschema in the project by uuid
	 * 
	 * @param project
	 * @param uuid
	 * @return
	 */
	HibMicroschema findByUuid(HibProject project, String uuid);

	/**
	 * Check whether the project contains the microschema.
	 * 
	 * @param project
	 * @param microschema
	 * @return
	 */
	boolean contains(HibProject project, HibMicroschema microschema);

	/**
	 * Add the microschema to the project.
	 * 
	 * @param project
	 * @param user
	 * @param microschemaContainer
	 * @param batch
	 */
	void addMicroschema(HibProject project, HibUser user, HibMicroschema microschemaContainer, EventQueueBatch batch);

	/**
	 * Remove the given microschema from the project.
	 * 
	 * @param project
	 * @param microschema
	 * @param batch
	 */
	void removeMicroschema(HibProject project, HibMicroschema microschema, EventQueueBatch batch);

}
