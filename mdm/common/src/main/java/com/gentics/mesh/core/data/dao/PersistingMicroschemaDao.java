package com.gentics.mesh.core.data.dao;

import static com.gentics.mesh.core.data.perm.InternalPermission.CREATE_PERM;
import static com.gentics.mesh.core.rest.error.Errors.conflict;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.event.Assignment.ASSIGNED;
import static com.gentics.mesh.event.Assignment.UNASSIGNED;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.stream.Stream;

import org.apache.commons.lang.NotImplementedException;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.node.HibMicronode;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.schema.HibMicroschema;
import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.data.schema.handler.FieldSchemaContainerComparator;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.CommonTx;
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
public interface PersistingMicroschemaDao 
		extends MicroschemaDao, 
			PersistingContainerDao<MicroschemaResponse, MicroschemaVersionModel, MicroschemaReference, HibMicroschema, HibMicroschemaVersion, MicroschemaModel>,
			ElementResolvingRootDao<HibProject, HibMicroschema>{

	/**
	 * Find all micronodes belonging to this microschema version
	 * 
	 * @param version
	 * @return
	 */
	Result<? extends HibMicronode> findMicronodes(HibMicroschemaVersion version);

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
	@Override
	default void assign(HibMicroschema microschemaContainer, HibProject project, HibUser user, EventQueueBatch batch) {
		PersistingProjectDao projectDao = CommonTx.get().projectDao();
		BranchDao branchDao = Tx.get().branchDao();

		branchDao.findAll(project).stream()
				.filter(branch -> branch.contains(microschemaContainer))
				.findAny()
				.ifPresentOrElse(existing -> {
					HibMicroschema.log.warn("Microschema { " + microschemaContainer.getName() + " } is already assigned to the project { " + project.getName() + " }");
				}, () -> {
					// Adding new microschema
					batch.add(projectDao.onMicroschemaAssignEvent(project, microschemaContainer, ASSIGNED));
					addItem(project, microschemaContainer);

					// Assign the latest microschema version to all branches of the project
					for (HibBranch branch : branchDao.findAll(project)) {
						branchDao.assignMicroschemaVersion(branch, user, microschemaContainer.getLatestVersion(), batch);
					}
				});
	}

	/**
	 * Remove the given microschema from the project.
	 *
	 * @param project
	 * @param microschema
	 * @param batch
	 */
	@Override
	default void unassign(HibMicroschema microschema, HibProject project, EventQueueBatch batch) {
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
	default HibMicroschema create(HibProject root, InternalActionContext ac, EventQueueBatch batch, String uuid) {
		HibMicroschema microschema = create(ac, batch, uuid);
		assign(microschema, root, ac.getUser(), batch);
		CommonTx.get().projectDao().mergeIntoPersisted(root);
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
		HibBaseElement microschemaRoot = CommonTx.get().data().permissionRoots().microschema();

		HibUser requestUser = ac.getUser();
		MicroschemaVersionModel microschema = JsonUtil.readValue(ac.getBodyAsString(), MicroschemaModelImpl.class);
		microschema.validate();
		if (!userRoot.hasPermission(requestUser, microschemaRoot, InternalPermission.CREATE_PERM)) {
			throw error(FORBIDDEN, "error_missing_perm", microschemaRoot.getUuid(),
				CREATE_PERM.getRestPerm().getName());
		}
		HibMicroschema container = create(microschema, requestUser, uuid, batch);
		userRoot.inheritRolePermissions(requestUser, microschemaRoot, container);
		mergeIntoPersisted(container);
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

		HibMicroschema container = createPersisted(uuid);
		HibMicroschemaVersion version = createPersistedVersion(container, v -> {
			// set the initial version
			microschema.setVersion("1.0");
			v.setName(microschema.getName());
			v.setSchema(microschema);
			v.setSchemaContainer(container);
		});
		container.setLatestVersion(version);
		container.setCreated(user);
		container.setName(microschema.getName());
		container.generateBucketId();

		return mergeIntoPersisted(container);
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
			if (findMicronodes(version).hasNext()) {
				throw error(BAD_REQUEST, "microschema_delete_still_in_use", microschema.getUuid());
			}
			deleteVersion(version, bac);
		}
		bac.add(microschema.onDeleted());
		deletePersisted(microschema);
	}

	@Override
	default MicroschemaResponse transformToRestSync(HibMicroschema element, InternalActionContext ac, int level,
			String... languageTags) {
		return element.transformToRestSync(ac, level, languageTags);
	}

	@Override
	default void delete(HibProject root, HibMicroschema element, BulkActionContext bac) {
		unassign(element, root, bac.batch());
		assignEvents(element, UNASSIGNED).forEach(bac::add);
		// TODO should we delete the schema completely?
		//delete(element, bac);
	}

	@Override
	default boolean update(HibMicroschema element, InternalActionContext ac, EventQueueBatch batch) {
		throw new NotImplementedException("Updating is not directly supported for microschemas. Please start a microschema migration");
	}

	@Override
	default boolean update(HibProject project, HibMicroschema element, InternalActionContext ac, EventQueueBatch batch) {
		// Don't update the item, if it does not belong to the requested root.
		if (project.getMicroschemas().stream().noneMatch(schema -> element.getUuid().equals(schema.getUuid()))) {
			throw error(NOT_FOUND, "object_not_found_for_uuid", element.getUuid());
		}
		return update(element, ac, batch);
	}

	@Override
	default boolean isLinkedToProject(HibMicroschema microschema, HibProject project) {
		return contains(project, microschema);
	}

	@Override
	default FieldSchemaContainerComparator<MicroschemaModel> getFieldSchemaContainerComparator() {
		return CommonTx.get().data().mesh().microschemaComparator();
	}
}
