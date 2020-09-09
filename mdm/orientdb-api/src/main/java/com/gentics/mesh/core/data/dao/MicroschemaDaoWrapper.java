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

public interface MicroschemaDaoWrapper extends MicroschemaDao, DaoWrapper<HibMicroschema> {

	HibMicroschema loadObjectByUuid(InternalActionContext ac, String schemaUuid, InternalPermission perm);

	HibMicroschema findByUuid(String uuid);

	// boolean update(MicroschemaContainer microschema, InternalActionContext ac,
	// EventQueueBatch batch);

	/**
	 * Create a new microschema container.
	 * 
	 * @param microschema
	 * @param user        User that is used to set creator and editor references.
	 * @param batch
	 * @return
	 */
	default HibMicroschema create(MicroschemaVersionModel microschema, HibUser user, EventQueueBatch batch) {
		return create(microschema, user, null, batch);
	}

	/**
	 * Create a new microschema container.
	 * 
	 * @param microschema
	 * @param user        User that is used to set creator and editor references.
	 * @param uuid        optional uuid
	 * @param batch
	 * @return
	 */
	HibMicroschema create(MicroschemaVersionModel microschema, HibUser user, String uuid, EventQueueBatch batch);

	HibMicroschema create(InternalActionContext ac, EventQueueBatch batch, String uuid);

	Page<? extends Microschema> findAll(InternalActionContext ac, PagingParameters pagingInfo);

	Page<? extends Microschema> findAll(InternalActionContext ac, PagingParameters pagingInfo,
			Predicate<Microschema> extraFilter);

	HibMicroschema loadObjectByUuid(InternalActionContext ac, String uuid, InternalPermission perm,
			boolean errorIfNotFound);

	HibMicroschema findByName(String name);

	Result<? extends Microschema> findAll();

	default HibMicroschemaVersion fromReference(MicroschemaReference reference) {
		return fromReference(null, reference);
	}

	/**
	 * Get the microschema container version from the given reference.
	 * 
	 * @param reference reference
	 * @return
	 */
	default HibMicroschemaVersion fromReference(HibProject project, MicroschemaReference reference) {
		return fromReference(project, reference, null);
	}

	/**
	 * Get the microschema container version from the given reference. Ignore the
	 * version number from the reference, but take the version from the branch
	 * instead.
	 * 
	 * @param project
	 * @param reference reference
	 * @param branch    branch
	 * @return
	 */
	HibMicroschemaVersion fromReference(HibProject project, MicroschemaReference reference, HibBranch branch);

	void delete(HibMicroschema microschema, BulkActionContext bac);

	boolean isLinkedToProject(HibMicroschema microschema, HibProject project);

	HibMicroschemaVersion applyChanges(HibMicroschemaVersion version, InternalActionContext ac, EventQueueBatch batch);

	HibMicroschemaVersion applyChanges(HibMicroschemaVersion version, InternalActionContext ac,
			SchemaChangesListModel model, EventQueueBatch batch);

	SchemaChangesListModel diff(HibMicroschemaVersion version, InternalActionContext ac, MicroschemaModel requestModel);

	Iterable<? extends HibMicroschemaVersion> findAllVersions(HibMicroschema microschema);

	Map<HibBranch, HibMicroschemaVersion> findReferencedBranches(HibMicroschema microschema);

	Result<? extends NodeGraphFieldContainer> findDraftFieldContainers(HibMicroschemaVersion version,
			String branchUuid);

	void unlink(HibMicroschema microschema, HibProject project, EventQueueBatch batch);

	MicroschemaResponse transformToRestSync(HibMicroschema microschema, InternalActionContext ac, int level,
			String... languageTags);

	String getETag(HibMicroschema schema, InternalActionContext ac);

	void addMicroschema(HibMicroschema schema, HibUser user, EventQueueBatch batch);

	Result<? extends HibMicroschema> findAll(HibProject project);

	Result<HibMicroschemaVersion> findActiveMicroschemaVersions(HibBranch branch);

	Page<? extends HibMicroschema> findAll(HibProject project, InternalActionContext ac,
			PagingParameters pagingInfo);

	HibMicroschema findByUuid(HibProject project, String uuid);

	boolean contains(HibProject project, HibMicroschema microschema);

	void addMicroschema(HibProject project, HibUser user, HibMicroschema microschemaContainer, EventQueueBatch batch);

	void removeMicroschema(HibProject project, HibMicroschema microschema, EventQueueBatch batch);

}
