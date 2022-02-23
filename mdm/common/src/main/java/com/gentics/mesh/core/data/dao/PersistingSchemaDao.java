package com.gentics.mesh.core.data.dao;

import static com.gentics.mesh.core.data.perm.InternalPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PUBLISHED_PERM;
import static com.gentics.mesh.core.rest.error.Errors.conflict;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.event.Assignment.ASSIGNED;
import static com.gentics.mesh.event.Assignment.UNASSIGNED;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.apache.commons.lang.NotImplementedException;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.schema.HibMicroschema;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.data.schema.handler.FieldSchemaContainerComparator;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.core.rest.event.project.ProjectSchemaEventModel;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.schema.SchemaVersionModel;
import com.gentics.mesh.core.rest.schema.impl.SchemaModelImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.core.result.TraversalResult;
import com.gentics.mesh.core.search.index.node.NodeIndexHandler;
import com.gentics.mesh.error.MeshSchemaException;
import com.gentics.mesh.event.Assignment;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.json.JsonUtil;

/**
 * A persisting extension to {@link SchemaDao}
 * 
 * @author plyhun
 *
 */
public interface PersistingSchemaDao 
			extends SchemaDao, 
			PersistingContainerDao<SchemaResponse, SchemaVersionModel, SchemaReference, HibSchema, HibSchemaVersion, SchemaModel> {

	/**
	 * Create the schema.
	 * 
	 * @param ac
	 * @param batch
	 * @param uuid
	 * @return
	 */
	default HibSchema create(InternalActionContext ac, EventQueueBatch batch, String uuid) {
		HibUser requestUser = ac.getUser();
		UserDao userDao = Tx.get().userDao();
		HibBaseElement schemaRoot = CommonTx.get().data().permissionRoots().schema();

		SchemaVersionModel requestModel = JsonUtil.readValue(ac.getBodyAsString(), SchemaModelImpl.class);
		requestModel.validate();

		if (!userDao.hasPermission(requestUser, schemaRoot, CREATE_PERM)) {
			throw error(FORBIDDEN, "error_missing_perm", schemaRoot.getUuid(), CREATE_PERM.getRestPerm().getName());
		}
		HibSchema container = create(requestModel, requestUser, uuid, ac.getSchemaUpdateParameters().isStrictValidation());
		userDao.inheritRolePermissions(requestUser, schemaRoot, container);
		container = mergeIntoPersisted(container);
		batch.add(container.onCreated());
		return container;
	}

	/**
	 * Find the referenced schema container version. Throws an error, if the referenced schema container version can not be found
	 * 
	 * @param reference
	 *            reference
	 * @return Resolved container version
	 */
	default HibSchemaVersion fromReference(SchemaReference reference) {
		return fromReference(null, reference);
	}

	/**
	 * Load the schema versions via the given reference.
	 * 
	 * @param project
	 * @param reference
	 * @return
	 */
	default HibSchemaVersion fromReference(HibProject project, SchemaReference reference) {
		if (reference == null) {
			throw error(INTERNAL_SERVER_ERROR, "Missing schema reference");
		}
		String schemaName = reference.getName();
		String schemaUuid = reference.getUuid();
		String schemaVersion = reference.getVersion();

		// Prefer the name over the uuid
		HibSchema schemaContainer = null;
		if (!isEmpty(schemaName)) {
			if (project != null) {
				schemaContainer = findByName(project, schemaName);
			} else {
				schemaContainer = findByName(schemaName);
			}
		} else {
			if (project != null) {
				schemaContainer = findByUuid(project, schemaUuid);
			} else {
				schemaContainer = findByUuid(schemaUuid);
			}
		}

		// Check whether a container was actually found
		if (schemaContainer == null) {
			throw error(BAD_REQUEST, "error_schema_reference_not_found", isEmpty(schemaName) ? "-" : schemaName, isEmpty(schemaUuid) ? "-"
				: schemaUuid, schemaVersion == null ? "-" : schemaVersion.toString());
		}
		if (schemaVersion == null) {
			return schemaContainer.getLatestVersion();
		} else {
			HibSchemaVersion foundVersion = findVersionByRev(schemaContainer, schemaVersion);
			if (foundVersion == null) {
				throw error(BAD_REQUEST, "error_schema_reference_not_found", isEmpty(schemaName) ? "-" : schemaName, isEmpty(schemaUuid) ? "-"
					: schemaUuid, schemaVersion == null ? "-" : schemaVersion.toString());
			} else {
				return foundVersion;
			}
		}
	}

	/**
	 * Create new schema container.
	 * 
	 * @param schema
	 *            Schema that should be stored in the container
	 * @param creator
	 *            User that is used to set editor and creator references
	 * @param uuid
	 *            Optional uuid
	 * @return Created schema container
	 * @throws MeshSchemaException
	 */
	default HibSchema create(SchemaVersionModel schema, HibUser creator, String uuid) {
		return create(schema, creator, uuid, false);
	}	

	/**
	 * Create new schema container.
	 * 
	 * @param schema
	 *            Schema that should be stored in the container
	 * @param creator
	 *            User that is used to set editor and creator references
	 * @return Created schema container
	 * @throws MeshSchemaException
	 */
	default HibSchema create(SchemaVersionModel schema, HibUser creator) throws MeshSchemaException {
		return create(schema, creator, null);
	}

	/**
	 * Create new schema container.
	 *
	 * @param schema
	 *            Schema that should be stored in the container
	 * @param creator
	 *            User that is used to set editor and creator references
	 * @param uuid
	 *            Optional uuid
	 * @param validate
	 *
	 * @return Created schema container
	 * @throws MeshSchemaException
	 */
	default HibSchema create(SchemaVersionModel schema, HibUser creator, String uuid, boolean validate) {
		MicroschemaDao microschemaDao = Tx.get().microschemaDao();

		// TODO FIXME - We need to skip the validation check if the instance is creating a clustered instance because vert.x is not yet ready.
		// https://github.com/gentics/mesh/issues/210
		if (validate && CommonTx.get().data().vertx() != null) {
			PersistingSchemaDao.validateSchema(CommonTx.get().data().mesh().nodeContainerIndexHandler(), schema);
		}

		String name = schema.getName();
		HibSchema conflictingSchema = findByName(name);
		if (conflictingSchema != null) {
			throw conflict(conflictingSchema.getUuid(), name, "schema_conflicting_name", name);
		}

		HibMicroschema conflictingMicroschema = microschemaDao.findByName(name);
		if (conflictingMicroschema != null) {
			throw conflict(conflictingMicroschema.getUuid(), name, "microschema_conflicting_name", name);
		}

		HibSchema container = createPersisted(uuid);
		HibSchemaVersion version = createPersistedVersion(container, v -> {
			// set the initial version
			schema.setVersion("1.0");
			v.setSchema(schema);
			v.setName(schema.getName());
			v.setSchemaContainer(container);			
		});
		container.setLatestVersion(version);
		container.setCreated(creator);
		container.setName(schema.getName());
		container.generateBucketId();

		return mergeIntoPersisted(container);
	}

	/**
	 * Find all projects which reference the schema.
	 * 
	 * @param schema
	 * @return
	 */
	default Result<HibProject> findLinkedProjects(HibSchema schema) {
		return new TraversalResult<>(Tx.get().projectDao()
				.findAll().stream().filter(project -> isLinkedToProject(schema, project)));
	}

	/**
	 * Returns events for assignment on the schema action.
	 * 
	 * @return
	 */
	default Stream<ProjectSchemaEventModel> assignEvents(HibSchema schema, Assignment assigned) {
		ProjectDao projectDao = Tx.get().projectDao();
		return findLinkedProjects(schema)
			.stream()
			.map(project -> projectDao.onSchemaAssignEvent(project, schema, assigned));
	}

	/**
	 * Assign the schema to the project.
	 * 
	 * @param schemaContainer
	 * @param project
	 * @param user
	 * @param batch
	 */
	@Override
	default void assign(HibSchema schemaContainer, HibProject project, HibUser user, EventQueueBatch batch) {
		ProjectDao projectDao = Tx.get().projectDao();
		BranchDao branchDao = Tx.get().branchDao();

		branchDao.findAll(project).stream()
			.filter(branch -> branch.contains(schemaContainer))
			.findAny()
			.ifPresentOrElse(existing -> {
				throw error(BAD_REQUEST, "schema_conflicting_name", existing.getUuid(),
						CREATE_PERM.getRestPerm().getName());
			}, () -> {
				// Adding new schema
				batch.add(projectDao.onSchemaAssignEvent(project, schemaContainer, ASSIGNED));
				addItem(project, schemaContainer);
	
				// Assign the latest schema version to all branches of the project
				for (HibBranch branch : branchDao.findAll(project)) {
					branchDao.assignSchemaVersion(branch, user, schemaContainer.getLatestVersion(), batch);
				}
			});
	}

	@Override
	default void unassign(HibSchema schema, HibProject project, EventQueueBatch batch) {
		ProjectDao projectDao = Tx.get().projectDao();
		BranchDao branchDao = Tx.get().branchDao();

		batch.add(projectDao.onSchemaAssignEvent(project, schema, UNASSIGNED));
		removeItem(project, schema);

		// unassign the schema from all branches
		for (HibBranch branch : branchDao.findAll(project)) {
			branch.unassignSchema(schema);
		}
	}

	@Override
	default void delete(HibSchema schema, BulkActionContext bac) {
		// Check whether the schema is currently being referenced by nodes.
		Iterator<? extends HibNode> it = getNodes(schema).iterator();
		if (!it.hasNext()) {

			assignEvents(schema, UNASSIGNED).forEach(bac::add);
			bac.add(schema.onDeleted());

			for (HibSchemaVersion v : findAllVersions(schema)) {
				deleteVersion(v, bac);
			}
			deletePersisted(schema);
		} else {
			throw error(BAD_REQUEST, "schema_delete_still_in_use", schema.getUuid());
		}
	}

	@Override
	default void delete(HibProject root, HibSchema element, BulkActionContext bac) {
		unassign(element, root, bac.batch());
		assignEvents(element, UNASSIGNED).forEach(bac::add);
		// TODO should we delete the schema completely?
		//delete(element, bac);
	}

	/**
	 * Validate the given schema model using the elasticsearch index handler (needed for ES setting validation).
	 * 
	 * @param indexHandler
	 * @param schema
	 */
	public static void validateSchema(NodeIndexHandler indexHandler, SchemaVersionModel schema) {
		// TODO Maybe set the timeout to the configured search.timeout? But the default of 60 seconds is really long.
		Throwable error = indexHandler.validate(schema).blockingGet(10, TimeUnit.SECONDS);

		if (error != null) {
			if (error instanceof GenericRestException) {
				throw (GenericRestException) error;
			} else {
				throw new RuntimeException(error);
			}
		}
	}

	@Override
	default HibSchema create(HibProject root, InternalActionContext ac, EventQueueBatch batch, String uuid) {
		HibSchema schema = create(ac, batch, uuid);
		assign(schema, root, ac.getUser(), batch);
		CommonTx.get().projectDao().mergeIntoPersisted(root);
		assignEvents(schema, UNASSIGNED).forEach(batch::add);
		return schema;
	}

	@Override
	default SchemaResponse transformToRestSync(HibSchema element, InternalActionContext ac, int level,
			String... languageTags) {
		return element.transformToRestSync(ac, level, languageTags);
	}

	@Override
	default boolean update(HibSchema element, InternalActionContext ac, EventQueueBatch batch) {
		throw new NotImplementedException("Updating is not directly supported for schemas. Please start a schema migration");
	}

	@Override
	default boolean update(HibProject project, HibSchema element, InternalActionContext ac, EventQueueBatch batch) {
		// Don't update the item, if it does not belong to the requested branch.
		if (project.getSchemas().stream().noneMatch(schema -> element.getUuid().equals(schema.getUuid()))) {
			throw error(NOT_FOUND, "object_not_found_for_uuid", element.getUuid());
		}
		return update(element, ac, batch);
	}

	@Override
	default boolean isLinkedToProject(HibSchema schema, HibProject project) {
		return contains(project, schema);
	}

	@Override
	default FieldSchemaContainerComparator<SchemaModel> getFieldSchemaContainerComparator() {
		return CommonTx.get().data().mesh().schemaComparator();
	}
}
