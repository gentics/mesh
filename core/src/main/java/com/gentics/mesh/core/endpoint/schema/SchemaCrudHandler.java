package com.gentics.mesh.core.endpoint.schema;

import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.UPDATE_PERM;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.rest.Messages.message;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NO_CONTENT;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.action.SchemaDAOActions;
import com.gentics.mesh.core.actions.impl.ProjectSchemaLoadAllActionImpl;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.BranchDao;
import com.gentics.mesh.core.data.dao.MicroschemaDao;
import com.gentics.mesh.core.data.dao.PersistingSchemaDao;
import com.gentics.mesh.core.data.dao.SchemaDao;
import com.gentics.mesh.core.data.dao.UserDao;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.schema.HibMicroschema;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.data.schema.handler.SchemaComparatorImpl;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.endpoint.handler.AbstractCrudHandler;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.MicronodeFieldSchema;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangesListModel;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaUpdateRequest;
import com.gentics.mesh.core.search.index.node.NodeIndexHandler;
import com.gentics.mesh.core.verticle.handler.HandlerUtilities;
import com.gentics.mesh.core.verticle.handler.WriteLock;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.parameter.SchemaUpdateParameters;
import com.gentics.mesh.util.UUIDUtil;

import dagger.Lazy;

/**
 * CRUD handler for schema REST operation.
 */
public class SchemaCrudHandler extends AbstractCrudHandler<HibSchema, SchemaResponse> {

	private SchemaComparatorImpl comparator;

	private Lazy<BootstrapInitializer> boot;

	private final NodeIndexHandler nodeIndexHandler;

	private final ProjectSchemaLoadAllActionImpl projectSchemaDAOActions;

	@Inject
	public SchemaCrudHandler(Database db, SchemaComparatorImpl comparator, Lazy<BootstrapInitializer> boot,
		HandlerUtilities utils, NodeIndexHandler nodeIndexHandler, WriteLock writeLock, ProjectSchemaLoadAllActionImpl projectSchemaDAOActions,
		SchemaDAOActions schemaActions) {
		super(db, utils, writeLock, schemaActions);
		this.comparator = comparator;
		this.boot = boot;
		this.nodeIndexHandler = nodeIndexHandler;
		this.projectSchemaDAOActions = projectSchemaDAOActions;
	}

	/**
	 * Update the schema in a blocking manner in order to keep the execution sequential.
	 */
	@Override
	public void handleUpdate(InternalActionContext ac, String uuid) {
		validateParameter(uuid, "uuid");
		try (WriteLock lock = writeLock.lock(ac)) {

			/**
			 * The following code delegates the call to the handleUpdate method is very hacky at best. It would be better to move the whole update code into the
			 * SchemaContainerImpl#update method and use the regular handlerUtilities. (similar to all other calls) The current code however does not return a
			 * SchemaResponse for update requests. Instead a message will be returned. Changing this behaviour would cause a breaking change. (Changed response
			 * model).
			 */
			boolean delegateToCreate = db.tx(tx -> {
				if (!UUIDUtil.isUUID(uuid)) {
					return false;
				}
				HibSchema schemaContainer = tx.schemaDao().findByUuid(uuid);
				return schemaContainer == null;
			});

			// Delegate to handle update which will create the schema
			if (delegateToCreate) {
				ac.skipWriteLock();
				super.handleUpdate(ac, uuid);
				return;
			}

			utils.syncTx(ac, tx1 -> {
				UserDao userDao = tx1.userDao();
				SchemaDao schemaDao = tx1.schemaDao();
				MicroschemaDao microschemaDao = tx1.microschemaDao();
				BranchDao branchDao = tx1.branchDao();

				// 1. Load the schema container with update permissions
				HibSchema schemaContainer = schemaDao.loadObjectByUuid(ac, uuid, UPDATE_PERM);
				SchemaUpdateRequest requestModel = JsonUtil.readValue(ac.getBodyAsString(), SchemaUpdateRequest.class);

				if (ac.getSchemaUpdateParameters().isStrictValidation()) {
					PersistingSchemaDao.validateSchema(nodeIndexHandler, requestModel);
				}

				// 2. Diff the schema with the latest version
				SchemaChangesListModel model = new SchemaChangesListModel();
				model.getChanges().addAll(comparator.diff(schemaContainer.getLatestVersion().getSchema(), requestModel));
				String schemaName = schemaContainer.getName();

				// No changes -> done
				if (model.getChanges().isEmpty()) {
					return message(ac, "schema_update_no_difference_detected", schemaName);
				}

				SchemaUpdateParameters updateParams = ac.getSchemaUpdateParameters();
				HibUser user = ac.getUser();
				String version = utils.eventAction(batch -> {

					// Check whether there are any microschemas which are referenced by the schema
					for (FieldSchema field : requestModel.getFields()) {
						if (field instanceof MicronodeFieldSchema) {
							MicronodeFieldSchema microschemaField = (MicronodeFieldSchema) field;

							String[] allowedSchemas = microschemaField.getAllowedMicroSchemas();
							if (allowedSchemas == null) {
								throw error(BAD_REQUEST, "schema_error_allowed_list_empty", microschemaField.getName());
							}

							// Check each allowed microschema individually
							for (String microschemaName : allowedSchemas) {

								// schema_error_microschema_reference_no_perm
								HibMicroschema microschema = boot.get().microschemaDao().findByName(microschemaName);
								if (microschema == null) {
									throw error(BAD_REQUEST, "schema_error_microschema_reference_not_found", microschemaName, field.getName());
								}
								if (!userDao.hasPermission(ac.getUser(), microschema, READ_PERM)) {
									throw error(BAD_REQUEST, "schema_error_microschema_reference_no_perm", microschemaName, field.getName());
								}

								// Locate the projects to which the schema was linked - We need to ensure that the microschema is also linked to those projects
								for (HibProject project : schemaDao.findLinkedProjects(schemaContainer)) {
									if (project != null) {
										microschemaDao.assign(microschema, project, user, batch);
									}
								}
							}
						}
					}

					// 3. Apply the found changes to the schema
					HibSchemaVersion createdVersion = schemaDao.applyChanges(schemaContainer.getLatestVersion(), ac, model, batch);

					// Check whether the assigned branches of the schema should also directly be updated.
					// This will trigger a node migration.
					if (updateParams.getUpdateAssignedBranches()) {
						Map<HibBranch, HibSchemaVersion> referencedBranches = schemaDao.findReferencedBranches(schemaContainer);

						// Assign the created version to the found branches
						for (Map.Entry<HibBranch, HibSchemaVersion> branchEntry : referencedBranches.entrySet()) {
							HibBranch branch = branchEntry.getKey();

							// Check whether a list of branch names was specified and skip branches which were not included in the list.
							List<String> branchNames = updateParams.getBranchNames();
							if (branchNames != null && !branchNames.isEmpty() && !branchNames.contains(branch.getName())) {
								continue;
							}

							// Assign the new version to the branch
							branchDao.assignSchemaVersion(branch, user, createdVersion, batch);
						}
						batch.add(() -> MeshEvent.triggerJobWorker(boot.get().mesh()));
					}
					return createdVersion.getVersion();
				});

				if (updateParams.getUpdateAssignedBranches()) {
					return message(ac, "schema_updated_migration_invoked", schemaName, version);
				} else {
					return message(ac, "schema_updated_migration_deferred", schemaName, version);
				}
			}, message -> ac.send(message, OK));
		}
	}

	/**
	 * Handle a schema diff request.
	 *
	 * @param ac
	 *            Context which contains the schema data to compare with
	 * @param uuid
	 *            Uuid of the schema which should also be used for comparison
	 */
	public void handleDiff(InternalActionContext ac, String uuid) {
		validateParameter(uuid, "uuid");

		utils.syncTx(ac, tx -> {
			SchemaDao schemaDao = tx.schemaDao();
			HibSchema schema = schemaDao.loadObjectByUuid(ac, uuid, READ_PERM);
			SchemaModel requestModel = JsonUtil.readValue(ac.getBodyAsString(), SchemaUpdateRequest.class);
			requestModel.validate();
			return schemaDao.diff(schema.getLatestVersion(), ac, requestModel);
		}, model -> ac.send(model, OK));
	}

	/**
	 * Handle a read project list request.
	 *
	 * @param ac
	 */
	public void handleReadProjectList(InternalActionContext ac) {
		utils.readElementList(ac, projectSchemaDAOActions);
	}

	/**
	 * Handle a add schema to project request.
	 *
	 * @param ac
	 *            Context which provides the project reference
	 * @param schemaUuid
	 *            Uuid of the schema which should be added to the project
	 */
	public void handleAddSchemaToProject(InternalActionContext ac, String schemaUuid) {
		validateParameter(schemaUuid, "schemaUuid");

		try (WriteLock lock = writeLock.lock(ac)) {
			utils.syncTx(ac, tx -> {
				UserDao userDao = tx.userDao();
				SchemaDao schemaDao = tx.schemaDao();

				HibProject project = tx.getProject(ac);
				String projectUuid = project.getUuid();
				if (!userDao.hasPermission(ac.getUser(), project, InternalPermission.UPDATE_PERM)) {
					throw error(FORBIDDEN, "error_missing_perm", projectUuid, UPDATE_PERM.getRestPerm().getName());
				}
				HibSchema schema = schemaDao.loadObjectByUuid(ac, schemaUuid, READ_PERM);
				if (schemaDao.isLinkedToProject(schema, project)) {
					// Schema has already been assigned. No need to create indices
					return schemaDao.transformToRestSync(schema, ac, 0);
				}

				// Assign the schema to the project
				utils.eventAction(batch -> {
					schemaDao.assign(schema, project, ac.getUser(), batch);
				});
				return schemaDao.transformToRestSync(schema, ac, 0);
			}, model -> ac.send(model, OK));
		}
	}

	/**
	 * Handle a remove schema from project request.
	 *
	 * @param ac
	 * @param schemaUuid
	 *            Uuid of the schema which should be removed from the project.
	 */
	public void handleRemoveSchemaFromProject(InternalActionContext ac, String schemaUuid) {
		validateParameter(schemaUuid, "schemaUuid");

		try (WriteLock lock = writeLock.lock(ac)) {
			utils.syncTx(ac, tx -> {
				SchemaDao schemaDao = tx.schemaDao();
				UserDao userDao = tx.userDao();

				HibProject project = tx.getProject(ac);
				String projectUuid = project.getUuid();

				if (!userDao.hasPermission(ac.getUser(), project, InternalPermission.UPDATE_PERM)) {
					throw error(FORBIDDEN, "error_missing_perm", projectUuid, UPDATE_PERM.getRestPerm().getName());
				}

				HibSchema schema = schemaDao.loadObjectByUuid(ac, schemaUuid, READ_PERM);

				// No need to invoke the removal if the schema is not assigned
				if (!schemaDao.isLinkedToProject(schema, project)) {
					return;
				}

				utils.eventAction(batch -> {
					schemaDao.unassign(schema, project, batch);
					batch.add(schema.onUpdated());
				});

			}, () -> ac.send(NO_CONTENT));
		}
	}

	/**
	 * Not yet implemented.
	 * 
	 * @deprecated Not yet implemented
	 * @param ac
	 */
	public void handleGetSchemaChanges(InternalActionContext ac) {
		// TODO Auto-generated method stub
	}

	/**
	 * Handle an apply changes to schema request.
	 *
	 * @param ac
	 *            Context which contains the changes request data
	 * @param schemaUuid
	 *            Uuid of the schema which should be modified
	 */
	public void handleApplySchemaChanges(InternalActionContext ac, String schemaUuid) {
		validateParameter(schemaUuid, "schemaUuid");

		try (WriteLock lock = writeLock.lock(ac)) {
			utils.syncTx(ac, tx -> {
				SchemaDao schemaDao = tx.schemaDao();
				HibSchema schema = schemaDao.loadObjectByUuid(ac, schemaUuid, UPDATE_PERM);
				String version = utils.eventAction(batch -> {
					HibSchemaVersion newVersion = schemaDao.applyChanges(schema.getLatestVersion(), ac, batch);
					return newVersion.getVersion();
				});
				return message(ac, "schema_changes_applied", schema.getName(), version);
			}, model -> ac.send(model, OK));
		}

	}

}
