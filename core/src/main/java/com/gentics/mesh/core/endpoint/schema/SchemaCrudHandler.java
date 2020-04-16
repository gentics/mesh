package com.gentics.mesh.core.endpoint.schema;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
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
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.root.SchemaContainerRoot;
import com.gentics.mesh.core.data.root.impl.SchemaContainerRootImpl;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.data.schema.handler.SchemaComparator;
import com.gentics.mesh.core.endpoint.handler.AbstractCrudHandler;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.MicronodeFieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangesListModel;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaUpdateRequest;
import com.gentics.mesh.core.verticle.handler.HandlerUtilities;
import com.gentics.mesh.core.verticle.handler.WriteLock;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.parameter.SchemaUpdateParameters;
import com.gentics.mesh.search.index.node.NodeIndexHandler;
import com.gentics.mesh.util.UUIDUtil;

import dagger.Lazy;

public class SchemaCrudHandler extends AbstractCrudHandler<SchemaContainer, SchemaResponse> {

	private SchemaComparator comparator;

	private Lazy<BootstrapInitializer> boot;

	private final NodeIndexHandler nodeIndexHandler;

	@Inject
	public SchemaCrudHandler(Database db, SchemaComparator comparator, Lazy<BootstrapInitializer> boot,
		HandlerUtilities utils, NodeIndexHandler nodeIndexHandler, WriteLock writeLock) {
		super(db, utils, writeLock);
		this.comparator = comparator;
		this.boot = boot;
		this.nodeIndexHandler = nodeIndexHandler;
	}

	@Override
	public RootVertex<SchemaContainer> getRootVertex(InternalActionContext ac) {
		return boot.get().schemaContainerRoot();
	}

	/**
	 * Update the schema in a blocking manner in order to keep the execution sequential.
	 */
	@Override
	public void handleUpdate(InternalActionContext ac, String uuid) {
		validateParameter(uuid, "uuid");
		try (WriteLock lock = globalLock.lock(ac)) {

			/**
			 * The following code delegates the call to the handleUpdate method is very hacky at best. It would be better to move the whole update code into the
			 * SchemaContainerImpl#update method and use the regular handlerUtilities. (similar to all other calls) The current code however does not return a
			 * SchemaResponse for update requests. Instead a message will be returned. Changing this behaviour would cause a breaking change. (Changed response
			 * model).
			 */
			boolean delegateToCreate = db.tx(() -> {
				RootVertex<SchemaContainer> root = getRootVertex(ac);
				if (!UUIDUtil.isUUID(uuid)) {
					return false;
				}
				SchemaContainer schemaContainer = root.findByUuid(uuid);
				return schemaContainer == null;
			});

			// Delegate to handle update which will create the schema
			if (delegateToCreate) {
				super.handleUpdate(ac, uuid);
				return;
			}

			utils.syncTx(ac, tx1 -> {

				// 1. Load the schema container with update permissions
				RootVertex<SchemaContainer> root = getRootVertex(ac);
				SchemaContainer schemaContainer = root.loadObjectByUuid(ac, uuid, UPDATE_PERM);
				SchemaUpdateRequest requestModel = JsonUtil.readValue(ac.getBodyAsString(), SchemaUpdateRequest.class);

				if (ac.getSchemaUpdateParameters().isStrictValidation()) {
					SchemaContainerRootImpl.validateSchema(nodeIndexHandler, requestModel);
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
				User user = ac.getUser();
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
								MicroschemaContainer microschema = boot.get().microschemaContainerRoot().findByName(microschemaName);
								if (microschema == null) {
									throw error(BAD_REQUEST, "schema_error_microschema_reference_not_found", microschemaName, field.getName());
								}
								if (!ac.getUser().hasPermission(microschema, READ_PERM)) {
									throw error(BAD_REQUEST, "schema_error_microschema_reference_no_perm", microschemaName, field.getName());
								}

								// Locate the projects to which the schema was linked - We need to ensure that the microschema is also linked to those projects
								for (SchemaContainerRoot roots : schemaContainer.getRoots()) {
									Project project = roots.getProject();
									if (project != null) {
										project.getMicroschemaContainerRoot().addMicroschema(user, microschema, batch);
									}
								}
							}
						}
					}

					// 3. Apply the found changes to the schema
					SchemaContainerVersion createdVersion = schemaContainer.getLatestVersion().applyChanges(ac, model, batch);

					// Check whether the assigned branches of the schema should also directly be updated.
					// This will trigger a node migration.
					if (updateParams.getUpdateAssignedBranches()) {
						Map<Branch, SchemaContainerVersion> referencedBranches = schemaContainer.findReferencedBranches();

						// Assign the created version to the found branches
						for (Map.Entry<Branch, SchemaContainerVersion> branchEntry : referencedBranches.entrySet()) {
							Branch branch = branchEntry.getKey();

							// Check whether a list of branch names was specified and skip branches which were not included in the list.
							List<String> branchNames = updateParams.getBranchNames();
							if (branchNames != null && !branchNames.isEmpty() && !branchNames.contains(branch.getName())) {
								continue;
							}

							// Assign the new version to the branch
							branch.assignSchemaVersion(user, createdVersion, batch);
						}
					}
					return createdVersion.getVersion();
				});

				if (updateParams.getUpdateAssignedBranches()) {
					MeshEvent.triggerJobWorker(boot.get().mesh());
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

		utils.syncTx(ac, (tx) -> {
			SchemaContainer schema = getRootVertex(ac).loadObjectByUuid(ac, uuid, READ_PERM);
			Schema requestModel = JsonUtil.readValue(ac.getBodyAsString(), SchemaUpdateRequest.class);
			requestModel.validate();
			return schema.getLatestVersion().diff(ac, comparator, requestModel);
		}, model -> ac.send(model, OK));
	}

	/**
	 * Handle a read project list request.
	 *
	 * @param ac
	 */
	public void handleReadProjectList(InternalActionContext ac) {
		utils.readElementList(ac, () -> ac.getProject().getSchemaContainerRoot());
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

		try (WriteLock lock = globalLock.lock(ac)) {
			utils.syncTx(ac, tx -> {
				Project project = ac.getProject();
				String projectUuid = project.getUuid();
				if (!ac.getUser().hasPermission(project, GraphPermission.UPDATE_PERM)) {
					throw error(FORBIDDEN, "error_missing_perm", projectUuid, UPDATE_PERM.getRestPerm().getName());
				}
				SchemaContainer schema = getRootVertex(ac).loadObjectByUuid(ac, schemaUuid, READ_PERM);
				SchemaContainerRoot root = project.getSchemaContainerRoot();
				if (root.contains(schema)) {
					// Schema has already been assigned. No need to create indices
					return schema.transformToRestSync(ac, 0);
				}

				// Assign the schema to the project
				utils.eventAction(batch -> {
					root.addSchemaContainer(ac.getUser(), schema, batch);
				});
				return schema.transformToRestSync(ac, 0);
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

		try (WriteLock lock = globalLock.lock(ac)) {
			utils.syncTx(ac, () -> {
				Project project = ac.getProject();
				String projectUuid = project.getUuid();
				if (!ac.getUser().hasPermission(project, GraphPermission.UPDATE_PERM)) {
					throw error(FORBIDDEN, "error_missing_perm", projectUuid, UPDATE_PERM.getRestPerm().getName());
				}

				SchemaContainer schema = boot.get().schemaContainerRoot().loadObjectByUuid(ac, schemaUuid, READ_PERM);

				// No need to invoke the removal if the schema is not assigned
				if (!project.getSchemaContainerRoot().contains(schema)) {
					return;
				}

				utils.eventAction(batch -> {
					project.getSchemaContainerRoot().removeSchemaContainer(schema, batch);
					batch.add(schema.onUpdated());
				});

			}, () -> ac.send(NO_CONTENT));
		}
	}

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

		try (WriteLock lock = globalLock.lock(ac)) {
			utils.syncTx(ac, (tx) -> {
				SchemaContainer schema = boot.get().schemaContainerRoot().loadObjectByUuid(ac, schemaUuid, UPDATE_PERM);
				String version = utils.eventAction(batch -> {
					SchemaContainerVersion newVersion = schema.getLatestVersion().applyChanges(ac, batch);
					return newVersion.getVersion();
				});
				return message(ac, "schema_changes_applied", schema.getName(), version);
			}, model -> ac.send(model, OK));
		}

	}

}
