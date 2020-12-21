package com.gentics.mesh.core.endpoint.microschema;

import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.UPDATE_PERM;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.rest.Messages.message;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NO_CONTENT;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.action.MicroschemaDAOActions;
import com.gentics.mesh.core.actions.impl.ProjectMicroschemaLoadAllActionImpl;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.MicroschemaDaoWrapper;
import com.gentics.mesh.core.data.dao.UserDaoWrapper;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.schema.HibMicroschema;
import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;
import com.gentics.mesh.core.data.schema.handler.MicroschemaComparatorImpl;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.endpoint.handler.AbstractCrudHandler;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaModelImpl;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaResponse;
import com.gentics.mesh.core.rest.schema.MicroschemaModel;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangesListModel;
import com.gentics.mesh.core.verticle.handler.HandlerUtilities;
import com.gentics.mesh.core.verticle.handler.WriteLock;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.parameter.SchemaUpdateParameters;
import com.gentics.mesh.util.UUIDUtil;

import dagger.Lazy;

/**
 * Handler for /api/v1/microschemas CRUD operations.
 */
public class MicroschemaCrudHandler extends AbstractCrudHandler<HibMicroschema, MicroschemaResponse> {

	private MicroschemaComparatorImpl comparator;

	private Lazy<BootstrapInitializer> boot;

	private final ProjectMicroschemaLoadAllActionImpl projectMicroschemaLoadAllAction;

	@Inject
	public MicroschemaCrudHandler(Database db, MicroschemaComparatorImpl comparator, Lazy<BootstrapInitializer> boot, HandlerUtilities utils,
		WriteLock writeLock, ProjectMicroschemaLoadAllActionImpl projectMicroschemaLoadAllAction, MicroschemaDAOActions microschemaActions) {
		super(db, utils, writeLock, microschemaActions);
		this.comparator = comparator;
		this.boot = boot;
		this.projectMicroschemaLoadAllAction = projectMicroschemaLoadAllAction;
	}

	@Override
	public void handleUpdate(InternalActionContext ac, String uuid) {
		validateParameter(uuid, "uuid");

		try (WriteLock lock = writeLock.lock(ac)) {
			/**
			 * The following code delegates the call to the handleUpdate method is very hacky at best. It would be better to move the whole update code into the
			 * MicroschemaContainerImpl#update method and use the regular handlerUtilities. (similar to all other calls) The current code however does not
			 * return a MicroschemaResponse for update requests. Instead a message will be returned. Changing this behaviour would cause a breaking change.
			 * (Changed response model).
			 */
			boolean delegateToCreate = db.tx(tx -> {
				if (!UUIDUtil.isUUID(uuid)) {
					return false;
				}
				MicroschemaDaoWrapper microschemaDao = tx.microschemaDao();
				HibMicroschema microschema = microschemaDao.findByUuid(uuid);
				return microschema == null;
			});

			// Delegate to handle update which will create the microschema
			if (delegateToCreate) {
				ac.skipWriteLock();
				super.handleUpdate(ac, uuid);
				return;
			}

			utils.syncTx(ac, tx -> {
				MicroschemaDaoWrapper microschemaDao = tx.microschemaDao();
				HibMicroschema microschema = microschemaDao.loadObjectByUuid(ac, uuid, UPDATE_PERM);
				MicroschemaModel requestModel = JsonUtil.readValue(ac.getBodyAsString(), MicroschemaModelImpl.class);
				requestModel.validate();

				SchemaChangesListModel model = new SchemaChangesListModel();
				model.getChanges().addAll(comparator.diff(microschema.getLatestVersion().getSchema(), requestModel));
				String name = microschema.getName();

				if (model.getChanges().isEmpty()) {
					return message(ac, "schema_update_no_difference_detected", name);
				}
				HibUser user = ac.getUser();
				SchemaUpdateParameters updateParams = ac.getSchemaUpdateParameters();
				String version = utils.eventAction(batch -> {
					HibMicroschemaVersion createdVersion = microschemaDao.applyChanges(microschema.getLatestVersion(), ac, model, batch);

					if (updateParams.getUpdateAssignedBranches()) {
						Map<HibBranch, HibMicroschemaVersion> referencedBranches = microschemaDao.findReferencedBranches(microschema);

						// Assign the created version to the found branches
						for (Map.Entry<HibBranch, HibMicroschemaVersion> branchEntry : referencedBranches.entrySet()) {
							HibBranch branch = branchEntry.getKey();

							// Check whether a list of branch names was specified and skip branches which were not included in the list.
							List<String> branchNames = updateParams.getBranchNames();
							if (branchNames != null && !branchNames.isEmpty() && !branchNames.contains(branch.getName())) {
								continue;
							}

							// Assign the new version to the branch
							branch.assignMicroschemaVersion(user, createdVersion, batch);
						}
						batch.add(() -> MeshEvent.triggerJobWorker(boot.get().mesh()));
					}
					return createdVersion.getVersion();
				});

				if (updateParams.getUpdateAssignedBranches()) {
					return message(ac, "schema_updated_migration_invoked", name, version);
				} else {
					return message(ac, "schema_updated_migration_deferred", name, version);
				}

			}, model -> ac.send(model, OK));
		}

	}

	/**
	 * Compare the latest schema version with the given schema model.
	 * 
	 * @param ac
	 * @param uuid
	 *            Schema uuid
	 */
	public void handleDiff(InternalActionContext ac, String uuid) {
		utils.syncTx(ac, tx -> {
			MicroschemaDaoWrapper microschemaDao = tx.microschemaDao();

			HibMicroschema microschema = microschemaDao.loadObjectByUuid(ac, uuid, READ_PERM);
			MicroschemaModel requestModel = JsonUtil.readValue(ac.getBodyAsString(), MicroschemaModelImpl.class);
			requestModel.validate();
			return microschemaDao.diff(microschema.getLatestVersion(), ac, requestModel);
		}, model -> ac.send(model, OK));
	}

	/**
	 * Handle a schema apply changes request.
	 * 
	 * @param ac
	 *            Context which contains the changes data
	 * @param schemaUuid
	 *            Schema which should be modified
	 */
	public void handleApplySchemaChanges(InternalActionContext ac, String schemaUuid) {
		try (WriteLock lock = writeLock.lock(ac)) {
			utils.syncTx(ac, tx -> {
				MicroschemaDaoWrapper microschemaDao = tx.microschemaDao();
				HibMicroschema schema = tx.microschemaDao().loadObjectByUuid(ac, schemaUuid, UPDATE_PERM);
				utils.eventAction(batch -> {
					microschemaDao.applyChanges(schema.getLatestVersion(), ac, batch);
				});
				return message(ac, "migration_invoked", schema.getName());
			}, model -> ac.send(model, OK));
		}
	}

	/**
	 * Handle a microschema read list request.
	 * 
	 * @param ac
	 */
	public void handleReadMicroschemaList(InternalActionContext ac) {
		utils.readElementList(ac, crudActions());
	}

	/**
	 * Handle a read project list request.
	 *
	 * @param ac
	 */
	public void handleReadProjectList(InternalActionContext ac) {
		utils.readElementList(ac, projectMicroschemaLoadAllAction);
	}

	/**
	 * Handle a request which will add a microschema to a project.
	 * 
	 * @param ac
	 *            Internal Action Context which also contains the project to which the microschema will be added.
	 * @param microschemaUuid
	 *            Microschema uuid which should be added to the project.
	 */
	public void handleAddMicroschemaToProject(InternalActionContext ac, String microschemaUuid) {
		validateParameter(microschemaUuid, "microschemaUuid");

		utils.syncTx(ac, tx -> {
			HibProject project = tx.getProject(ac);
			UserDaoWrapper userDao = tx.userDao();
			MicroschemaDaoWrapper microschemaDao = tx.microschemaDao();

			if (!userDao.hasPermission(ac.getUser(), project, UPDATE_PERM)) {
				String projectUuid = project.getUuid();
				throw error(FORBIDDEN, "error_missing_perm", projectUuid, UPDATE_PERM.getRestPerm().getName());
			}
			HibMicroschema microschema = tx.microschemaDao().loadObjectByUuid(ac, microschemaUuid, READ_PERM);

			// Only assign if the microschema has not already been assigned.
			if (!microschemaDao.contains(project, microschema)) {
				// Assign the microschema to the project
				utils.eventAction(batch -> {
					microschemaDao.addMicroschema(project, ac.getUser(), microschema, batch);
				});
			}
			return microschemaDao.transformToRestSync(microschema, ac, 0);
		}, model -> ac.send(model, OK));
	}

	/**
	 * Handle the un-assign for microschemas from a project.
	 * 
	 * @param ac Context which references the project
	 * @param microschemaUuid
	 */
	public void handleRemoveMicroschemaFromProject(InternalActionContext ac, String microschemaUuid) {
		validateParameter(microschemaUuid, "microschemaUuid");

		utils.syncTx(ac, tx -> {
			MicroschemaDaoWrapper microschemaDao = tx.microschemaDao();
			UserDaoWrapper userDao = tx.userDao();

			HibProject project = tx.getProject(ac);
			String projectUuid = project.getUuid();
			if (!userDao.hasPermission(ac.getUser(), project, UPDATE_PERM)) {
				throw error(FORBIDDEN, "error_missing_perm", projectUuid, UPDATE_PERM.getRestPerm().getName());
			}
			HibMicroschema microschema = tx.microschemaDao().loadObjectByUuid(ac, microschemaUuid, READ_PERM);
			if (microschemaDao.isLinkedToProject(microschema, project)) {
				utils.eventAction(batch -> {
					// Remove the microschema from the project
					microschemaDao.unlink(microschema, project, batch);
				});
			}
		}, () -> ac.send(NO_CONTENT));
	}
}
