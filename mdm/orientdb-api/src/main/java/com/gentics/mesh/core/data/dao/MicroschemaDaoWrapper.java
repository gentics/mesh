package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.data.schema.MicroschemaContainerVersion;
import com.gentics.mesh.core.rest.microschema.MicroschemaModel;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.madl.traversal.TraversalResult;
import com.gentics.mesh.parameter.PagingParameters;

public interface MicroschemaDaoWrapper extends MicroschemaDao {

	MicroschemaContainer loadObjectByUuid(InternalActionContext ac, String schemaUuid, GraphPermission perm);

	MicroschemaContainer findByUuid(String uuid);

//	boolean update(MicroschemaContainer microschema, InternalActionContext ac, EventQueueBatch batch);

	/**
	 * Create a new microschema container.
	 * 
	 * @param microschema
	 * @param user
	 *            User that is used to set creator and editor references.
	 * @param batch
	 * @return
	 */
	default MicroschemaContainer create(MicroschemaModel microschema, User user, EventQueueBatch batch) {
		return create(microschema, user, null, batch);
	}

	/**
	 * Create a new microschema container.
	 * 
	 * @param microschema
	 * @param user
	 *            User that is used to set creator and editor references.
	 * @param uuid
	 *            optional uuid
	 * @param batch
	 * @return
	 */
	MicroschemaContainer create(MicroschemaModel microschema, User user, String uuid, EventQueueBatch batch);

	MicroschemaContainer create(InternalActionContext ac, EventQueueBatch batch, String uuid);

	TransformablePage<? extends MicroschemaContainer> findAll(InternalActionContext ac, PagingParameters pagingInfo);

	MicroschemaContainer loadObjectByUuid(InternalActionContext ac, String uuid, GraphPermission perm, boolean errorIfNotFound);

	MicroschemaContainer findByName(String name);

	TraversalResult<? extends MicroschemaContainer> findAll();

	default MicroschemaContainerVersion fromReference(MicroschemaReference reference) {
		return fromReference(null, reference);
	}

	/**
	 * Get the microschema container version from the given reference.
	 * 
	 * @param reference
	 *            reference
	 * @return
	 */
	default MicroschemaContainerVersion fromReference(Project project, MicroschemaReference reference) {
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
	MicroschemaContainerVersion fromReference(Project project, MicroschemaReference reference, Branch branch);

}
