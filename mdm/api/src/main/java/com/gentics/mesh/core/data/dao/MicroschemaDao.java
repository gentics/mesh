package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.schema.HibMicroschema;
import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.rest.microschema.MicroschemaVersionModel;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaResponse;
import com.gentics.mesh.core.rest.schema.MicroschemaModel;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;
import com.gentics.mesh.event.EventQueueBatch;

/**
 * DAO for microschema operations.
 */
public interface MicroschemaDao extends ContainerDao<MicroschemaResponse, MicroschemaVersionModel, MicroschemaReference, HibMicroschema, HibMicroschemaVersion, MicroschemaModel>, RootDao<HibProject, HibMicroschema> {

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
	 * Add the microschema to the database.
	 * 
	 * @param schema
	 * @param user
	 * @param batch
	 */
	void addMicroschema(HibMicroschema schema, HibUser user, EventQueueBatch batch);

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
