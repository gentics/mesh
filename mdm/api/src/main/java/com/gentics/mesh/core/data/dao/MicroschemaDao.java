package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.branch.Branch;
import com.gentics.mesh.core.data.project.Project;
import com.gentics.mesh.core.data.schema.Microschema;
import com.gentics.mesh.core.data.schema.MicroschemaVersion;
import com.gentics.mesh.core.data.user.User;
import com.gentics.mesh.core.rest.microschema.MicroschemaVersionModel;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaResponse;
import com.gentics.mesh.core.rest.schema.MicroschemaModel;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;
import com.gentics.mesh.event.EventQueueBatch;

/**
 * DAO for microschema operations.
 */
public interface MicroschemaDao extends ContainerDao<MicroschemaResponse, MicroschemaVersionModel, MicroschemaReference, Microschema, MicroschemaVersion, MicroschemaModel>, RootDao<Project, Microschema> {

	/**
	 * Create a new microschema container.
	 * 
	 * @param microschema
	 * @param user
	 *            User that is used to set creator and editor references.
	 * @param batch
	 * @return
	 */
	default Microschema create(MicroschemaVersionModel microschema, User user, EventQueueBatch batch) {
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
	Microschema create(MicroschemaVersionModel microschema, User user, String uuid, EventQueueBatch batch);

	/**
	 * Create a new microschema.
	 * 
	 * @param ac
	 * @param batch
	 * @param uuid
	 * @return
	 */
	Microschema create(InternalActionContext ac, EventQueueBatch batch, String uuid);

	/**
	 * Load the microschema version via the given reference.
	 * 
	 * @param reference
	 * @return
	 */
	default MicroschemaVersion fromReference(MicroschemaReference reference) {
		return fromReference(null, reference);
	}

	/**
	 * Get the microschema container version from the given reference.
	 * 
	 * @param reference
	 *            reference
	 * @return
	 */
	default MicroschemaVersion fromReference(Project project, MicroschemaReference reference) {
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
	MicroschemaVersion fromReference(Project project, MicroschemaReference reference, Branch branch);
}
