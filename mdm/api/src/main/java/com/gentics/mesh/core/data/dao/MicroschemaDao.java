package com.gentics.mesh.core.data.dao;

import static com.gentics.mesh.core.data.perm.InternalPermission.CREATE_PERM;
import static com.gentics.mesh.core.rest.error.Errors.conflict;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.Map;

import org.apache.commons.lang.NotImplementedException;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.schema.HibMicroschema;
import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.microschema.MicroschemaVersionModel;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaModelImpl;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaResponse;
import com.gentics.mesh.core.rest.schema.MicroschemaModel;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangesListModel;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.json.JsonUtil;

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

	/**
	 * Delete microschema version, notifying context if necessary.
	 * 
	 * @param version
	 * @param bac
	 */
	void deleteVersion(HibMicroschemaVersion version, BulkActionContext bac);

	/**
	 * Find the version of the microschema.
	 * 
	 * @param hibMicroschema
	 * @param version
	 * @return
	 */
	HibMicroschemaVersion findVersionByRev(HibMicroschema hibMicroschema, String version);

	@Override
	default String getAPIPath(HibMicroschema element, InternalActionContext ac) {
		return element.getAPIPath(ac);
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
		delete(element, bac);
	}

	@Override
	default boolean update(HibProject root, HibMicroschema element, InternalActionContext ac, EventQueueBatch batch) {
		throw new NotImplementedException("Updating is not directly supported for microschemas. Please start a microschema migration");
	}
}
