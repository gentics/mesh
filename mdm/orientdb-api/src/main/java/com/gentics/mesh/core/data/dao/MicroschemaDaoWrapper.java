package com.gentics.mesh.core.data.dao;

import java.util.function.Predicate;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.schema.Microschema;
import com.gentics.mesh.core.data.schema.MicroschemaVersion;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.rest.microschema.MicroschemaVersionModel;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.madl.traversal.TraversalResult;
import com.gentics.mesh.parameter.PagingParameters;

public interface MicroschemaDaoWrapper extends MicroschemaDao {

	Microschema loadObjectByUuid(InternalActionContext ac, String schemaUuid, GraphPermission perm);

	Microschema findByUuid(String uuid);

	// boolean update(MicroschemaContainer microschema, InternalActionContext ac, EventQueueBatch batch);

	/**
	 * Create a new microschema container.
	 * 
	 * @param microschema
	 * @param user
	 *            User that is used to set creator and editor references.
	 * @param batch
	 * @return
	 */
	default Microschema create(MicroschemaVersionModel microschema, HibUser user, EventQueueBatch batch) {
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
	Microschema create(MicroschemaVersionModel microschema, HibUser user, String uuid, EventQueueBatch batch);

	Microschema create(InternalActionContext ac, EventQueueBatch batch, String uuid);

	TransformablePage<? extends Microschema> findAll(InternalActionContext ac, PagingParameters pagingInfo);

	Page<? extends Microschema> findAll(InternalActionContext ac, PagingParameters pagingInfo, Predicate<Microschema> extraFilter);

	Microschema loadObjectByUuid(InternalActionContext ac, String uuid, GraphPermission perm, boolean errorIfNotFound);

	Microschema findByName(String name);

	TraversalResult<? extends Microschema> findAll();

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
