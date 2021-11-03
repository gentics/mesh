package com.gentics.mesh.core.data.dao;

import static com.gentics.mesh.core.data.perm.InternalPermission.CREATE_PERM;
import static com.gentics.mesh.core.rest.error.Errors.conflict;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.event.Assignment.ASSIGNED;
import static com.gentics.mesh.event.Assignment.UNASSIGNED;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.stream.Stream;

import org.apache.commons.lang.NotImplementedException;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.job.HibJob;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.schema.HibMicroschema;
import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.data.schema.HibSchemaChange;
import com.gentics.mesh.core.data.schema.handler.FieldSchemaContainerComparator;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.event.project.ProjectMicroschemaEventModel;
import com.gentics.mesh.core.rest.microschema.MicroschemaVersionModel;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaModelImpl;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaResponse;
import com.gentics.mesh.core.rest.schema.MicroschemaModel;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.core.result.TraversalResult;
import com.gentics.mesh.event.Assignment;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.json.JsonUtil;

/**
 * A persisting extension to {@link MicroschemaDao}
 * 
 * @author plyhun
 *
 */
public interface PersistingMicroschemaDao extends MicroschemaDao, PersistingDaoGlobal<HibMicroschema> {

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
	 * Add the microschema to the database.
	 *
	 * @param microschema
	 * @param user
	 * @param batch
	 */
	void addMicroschema(HibMicroschema microschema, HibUser user, EventQueueBatch batch);

	/**
	 * Check whether the project contains the microschema.
	 *
	 * @param project
	 * @param microschema
	 * @return
	 */
	boolean contains(HibProject project, HibMicroschema microschema);

	/**
	 * Find all projects which reference the schema.
	 * 
	 * @param schema
	 * @return
	 */
	default Result<HibProject> findLinkedProjects(HibMicroschema schema) {
		return new TraversalResult<>(Tx.get().projectDao()
				.findAll().stream().filter(project -> isLinkedToProject(schema, project)));
	}

	/**
	 * Add the microschema to the project.
	 *
	 * @param project
	 * @param user
	 * @param microschemaContainer
	 * @param batch
	 */
	default void addMicroschema(HibProject project, HibUser user, HibMicroschema microschemaContainer, EventQueueBatch batch) {
		ProjectDao projectDao = Tx.get().projectDao();
		BranchDao branchDao = Tx.get().branchDao();

		batch.add(projectDao.onMicroschemaAssignEvent(project, microschemaContainer, ASSIGNED));
		addItem(project, microschemaContainer);

		// assign the latest microschema version to all branches of the project
		for (HibBranch branch : branchDao.findAll(project)) {
			branch.assignMicroschemaVersion(user, microschemaContainer.getLatestVersion(), batch);
		}
	}

	/**
	 * Remove the given microschema from the project.
	 *
	 * @param project
	 * @param microschema
	 * @param batch
	 */
	default void removeMicroschema(HibProject project, HibMicroschema microschema, EventQueueBatch batch) {
		ProjectDao projectDao = Tx.get().projectDao();
		BranchDao branchDao = Tx.get().branchDao();

		batch.add(projectDao.onMicroschemaAssignEvent(project, microschema, UNASSIGNED));
		removeItem(project, microschema);

		// unassign the microschema from all branches
		for (HibBranch branch : branchDao.findAll(project)) {
			branch.unassignMicroschema(microschema);
		}
	}

	@Override
	default void deleteVersion(HibMicroschemaVersion version, BulkActionContext bac) {
		// Delete change
		HibSchemaChange<?> change = version.getNextChange();
		if (change != null) {
			deleteChange(change, bac);
		}
		// Delete referenced jobs
		for (HibJob job : version.referencedJobsViaFrom()) {
			job.remove();
		}
		for (HibJob job : version.referencedJobsViaTo()) {
			job.remove();
		}
		// Delete version
		Tx.get().delete(version, version.getClass());
	}

	@Override
	default String getAPIPath(HibMicroschema element, InternalActionContext ac) {
		return element.getAPIPath(ac);
	}

	@Override
	default HibMicroschema create(HibProject root, InternalActionContext ac, EventQueueBatch batch, String uuid) {
		HibMicroschema microschema = create(ac, batch, uuid);
		addMicroschema(root, ac.getUser(), microschema, batch);
		Tx.get().persist(root, Tx.get().projectDao());
		return microschema;
	}

	/**
	 * Create a new microschema.
	 *
	 * @param ac
	 * @param batch
	 * @param uuid
	 * @return
	 */
	default HibMicroschema create(InternalActionContext ac, EventQueueBatch batch, String uuid) {
		UserDao userRoot = Tx.get().userDao();
		HibBaseElement microschemaRoot = Tx.get().data().permissionRoots().microschema();

		HibUser requestUser = ac.getUser();
		MicroschemaVersionModel microschema = JsonUtil.readValue(ac.getBodyAsString(), MicroschemaModelImpl.class);
		microschema.validate();
		if (!userRoot.hasPermission(requestUser, microschemaRoot, InternalPermission.CREATE_PERM)) {
			throw error(FORBIDDEN, "error_missing_perm", microschemaRoot.getUuid(),
				CREATE_PERM.getRestPerm().getName());
		}
		HibMicroschema container = create(microschema, requestUser, uuid, batch);
		userRoot.inheritRolePermissions(requestUser, microschemaRoot, container);
		container = Tx.get().persist(container, this);
		batch.add(container.onCreated());
		return container;
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
	default HibMicroschema create(MicroschemaVersionModel microschema, HibUser user, String uuid,
		EventQueueBatch batch) {
		microschema.validate();

		SchemaDao schemaDao = Tx.get().schemaDao();

		String name = microschema.getName();
		HibMicroschema conflictingMicroSchema = findByName(name);
		if (conflictingMicroSchema != null) {
			throw conflict(conflictingMicroSchema.getUuid(), name, "microschema_conflicting_name", name);
		}

		HibSchema conflictingSchema = schemaDao.findByName(name);
		if (conflictingSchema != null) {
			throw conflict(conflictingSchema.getUuid(), name, "schema_conflicting_name", name);
		}

		HibMicroschema container = Tx.get().create(uuid, this);
		HibMicroschemaVersion version = container.getLatestVersion();

		microschema.setVersion("1.0");
		container.setLatestVersion(version);
		version.setName(microschema.getName());
		version.setSchema(microschema);
		version.setSchemaContainer(container);
		container.setCreated(user);
		container.setName(microschema.getName());
		container.generateBucketId();
		addMicroschema(container, user, batch);

		return Tx.get().persist(container, this);
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
	default HibMicroschemaVersion fromReference(HibProject project, MicroschemaReference reference, HibBranch branch) {
		String microschemaName = reference.getName();
		String microschemaUuid = reference.getUuid();
		String version = branch == null ? reference.getVersion() : null;
		HibMicroschema container = null;
		if (!isEmpty(microschemaName)) {
			if (project != null) {
				container = findByName(project, microschemaName);
			} else {
				container = findByName(microschemaName);
			}
		} else {
			if (project != null) {
				container = findByUuid(project, microschemaUuid);
			} else {
				container = findByUuid(microschemaUuid);
			}
		}
		// Return the specified version or fallback to latest version.
		if (container == null) {
			throw error(BAD_REQUEST, "error_microschema_reference_not_found",
				isEmpty(microschemaName) ? "-" : microschemaName, isEmpty(microschemaUuid) ? "-" : microschemaUuid,
				version == null ? "-" : version.toString());
		}

		HibMicroschemaVersion foundVersion = null;

		if (branch != null) {
			foundVersion = branch.findLatestMicroschemaVersion(container);
		} else if (version != null) {
			foundVersion = findVersionByRev(container, version);
		} else {
			foundVersion = container.getLatestVersion();
		}

		if (foundVersion == null) {
			throw error(BAD_REQUEST, "error_microschema_reference_not_found",
				isEmpty(microschemaName) ? "-" : microschemaName, isEmpty(microschemaUuid) ? "-" : microschemaUuid,
				version == null ? "-" : version.toString());
		}
		return foundVersion;
	}

	/**
	 * Returns events for assignment on the schema action.
	 * 
	 * @return
	 */
	default Stream<ProjectMicroschemaEventModel> assignEvents(HibMicroschema microschema, Assignment assigned) {
		ProjectDao projectDao = Tx.get().projectDao();
		return findLinkedProjects(microschema)
			.stream()
			.map(project -> projectDao.onMicroschemaAssignEvent(project, microschema, assigned));
	}

	@Override
	default void delete(HibMicroschema microschema, BulkActionContext bac) {
		for (HibMicroschemaVersion version : findAllVersions(microschema)) {
			if (version.findMicronodes().hasNext()) {
				throw error(BAD_REQUEST, "microschema_delete_still_in_use", microschema.getUuid());
			}
			deleteVersion(version, bac);
		}
		bac.add(microschema.onDeleted());
		Tx.get().delete(microschema, this);
	}

	@Override
	default MicroschemaResponse transformToRestSync(HibMicroschema element, InternalActionContext ac, int level,
			String... languageTags) {
		return element.transformToRestSync(ac, level, languageTags);
	}

	@Override
	default void delete(HibProject root, HibMicroschema element, BulkActionContext bac) {
		removeMicroschema(root, element, bac.batch());
		assignEvents(element, UNASSIGNED).forEach(bac::add);
		// TODO should we delete the schema completely?
		//delete(element, bac);
	}

	@Override
	default boolean update(HibMicroschema element, InternalActionContext ac, EventQueueBatch batch) {
		throw new NotImplementedException("Updating is not directly supported for microschemas. Please start a microschema migration");
	}

	@Override
	default boolean update(HibProject root, HibMicroschema element, InternalActionContext ac, EventQueueBatch batch) {
		return update(element, ac, batch);
	}

	@Override
	default boolean isLinkedToProject(HibMicroschema microschema, HibProject project) {
		return contains(project, microschema);
	}

	@Override
	default void unlink(HibMicroschema microschema, HibProject project, EventQueueBatch batch) {
		removeMicroschema(project, microschema, batch);
	}

	@Override
	default FieldSchemaContainerComparator<MicroschemaModel> getFieldSchemaContainerComparator() {
		return Tx.get().data().microschemaComparator();
	}
}
