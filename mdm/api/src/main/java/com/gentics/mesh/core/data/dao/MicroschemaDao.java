package com.gentics.mesh.core.data.dao;

import java.util.Map;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.root.RootDao;
import com.gentics.mesh.core.data.schema.HibMicroschema;
import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.rest.microschema.MicroschemaVersionModel;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaResponse;
import com.gentics.mesh.core.rest.schema.MicroschemaModel;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangesListModel;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.event.EventQueueBatch;

/**
 * DAO for microschema operations.
 */
public interface MicroschemaDao extends DaoGlobal<HibMicroschema>, DaoTransformable<HibMicroschema, MicroschemaResponse>, RootDao<HibProject, HibMicroschema> {

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
	 * Unlink the microschema from the project.
	 * 
	 * @param microschema
	 * @param project
	 * @param batch
	 */
	void unlink(HibMicroschema microschema, HibProject project, EventQueueBatch batch);

	/**
	 * Add the microschema to the database.
	 * 
	 * @param schema
	 * @param user
	 * @param batch
	 */
	void addMicroschema(HibMicroschema schema, HibUser user, EventQueueBatch batch);

	/**
	 * Find all microschema versions for the given branch.
	 * 
	 * @param branch
	 * @return
	 */
	Result<HibMicroschemaVersion> findActiveMicroschemaVersions(HibBranch branch);

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

	/**
	 * Return all draft contents which reference the microschema version.
	 * 
	 * @param version
	 * @param branchUuid
	 * @return
	 */
	Result<? extends HibNodeFieldContainer> findDraftFieldContainers(HibMicroschemaVersion version,
		String branchUuid);

	@Override
	default String getAPIPath(HibMicroschema element, InternalActionContext ac) {
		return element.getAPIPath(ac);
	}
}
