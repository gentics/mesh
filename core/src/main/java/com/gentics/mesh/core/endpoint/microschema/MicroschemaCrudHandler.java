package com.gentics.mesh.core.endpoint.microschema;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.rest.Messages.message;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NO_CONTENT;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.root.MicroschemaContainerRoot;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.data.schema.MicroschemaContainerVersion;
import com.gentics.mesh.core.data.schema.handler.MicroschemaComparator;
import com.gentics.mesh.core.endpoint.handler.AbstractCrudHandler;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaModelImpl;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaResponse;
import com.gentics.mesh.core.rest.schema.Microschema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangesListModel;
import com.gentics.mesh.core.verticle.handler.HandlerUtilities;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.event.impl.EventQueueBatchImpl;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.parameter.SchemaUpdateParameters;
import com.gentics.mesh.util.Tuple;

import dagger.Lazy;
import io.reactivex.Single;

public class MicroschemaCrudHandler extends AbstractCrudHandler<MicroschemaContainer, MicroschemaResponse> {

	private MicroschemaComparator comparator;

	private Lazy<BootstrapInitializer> boot;

	@Inject
	public MicroschemaCrudHandler(Database db, MicroschemaComparator comparator, Lazy<BootstrapInitializer> boot, HandlerUtilities utils) {
		super(db, utils);
		this.comparator = comparator;
		this.boot = boot;
	}

	@Override
	public RootVertex<MicroschemaContainer> getRootVertex(InternalActionContext ac) {
		return boot.get().microschemaContainerRoot();
	}

	@Override
	public void handleUpdate(InternalActionContext ac, String uuid) {
		validateParameter(uuid, "uuid");

		utils.asyncTx(ac, () -> {

			RootVertex<MicroschemaContainer> root = getRootVertex(ac);
			MicroschemaContainer schemaContainer = root.loadObjectByUuid(ac, uuid, UPDATE_PERM);
			Microschema requestModel = JsonUtil.readValue(ac.getBodyAsString(), MicroschemaModelImpl.class);
			requestModel.validate();

			SchemaChangesListModel model = new SchemaChangesListModel();
			model.getChanges().addAll(comparator.diff(schemaContainer.getLatestVersion().getSchema(), requestModel));
			String name = schemaContainer.getName();

			if (model.getChanges().isEmpty()) {
				return message(ac, "schema_update_no_difference_detected", name);
			}
			User user = ac.getUser();
			SchemaUpdateParameters updateParams = ac.getSchemaUpdateParameters();
			Tuple<EventQueueBatch, String> info = db.tx(() -> {
				EventQueueBatch batch = EventQueueBatch.create();
				MicroschemaContainerVersion createdVersion = schemaContainer.getLatestVersion().applyChanges(ac, model, batch);

				if (updateParams.getUpdateAssignedBranches()) {
					Map<Branch, MicroschemaContainerVersion> referencedBranches = schemaContainer.findReferencedBranches();

					// Assign the created version to the found branches
					for (Map.Entry<Branch, MicroschemaContainerVersion> branchEntry : referencedBranches.entrySet()) {
						Branch branch = branchEntry.getKey();

						// Check whether a list of branch names was specified and skip branches which were not included in the list.
						List<String> branchNames = updateParams.getBranchNames();
						if (branchNames != null && !branchNames.isEmpty() && !branchNames.contains(branch.getName())) {
							continue;
						}

						// Assign the new version to the branch
						branch.assignMicroschemaVersion(user, createdVersion);
					}
				}
				return Tuple.tuple(batch, createdVersion.getVersion());
			});

			info.v1().dispatch();
			if (updateParams.getUpdateAssignedBranches()) {
				MeshEvent.triggerJobWorker();
				return message(ac, "schema_updated_migration_invoked", name, info.v2());
			} else {
				return message(ac, "schema_updated_migration_deferred", name, info.v2());
			}

		}, model -> ac.send(model, OK));

	}

	/**
	 * Compare the latest schema version with the given schema model.
	 * 
	 * @param ac
	 * @param uuid
	 *            Schema uuid
	 */
	public void handleDiff(InternalActionContext ac, String uuid) {
		utils.asyncTx(ac, () -> {
			MicroschemaContainer microschema = getRootVertex(ac).loadObjectByUuid(ac, uuid, READ_PERM);
			Microschema requestModel = JsonUtil.readValue(ac.getBodyAsString(), MicroschemaModelImpl.class);
			requestModel.validate();
			return microschema.getLatestVersion().diff(ac, comparator, requestModel);
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
		utils.asyncTx(ac, () -> {
			MicroschemaContainer schema = boot.get().microschemaContainerRoot().loadObjectByUuid(ac, schemaUuid, UPDATE_PERM);
			db.tx(() -> {
				EventQueueBatch batch = EventQueueBatch.create();
				schema.getLatestVersion().applyChanges(ac, batch);
				return batch;
			}).dispatch();
			return message(ac, "migration_invoked", schema.getName());
		}, model -> ac.send(model, OK));

	}

	/**
	 * Handle a microschema read list request.
	 * 
	 * @param ac
	 */
	public void handleReadMicroschemaList(InternalActionContext ac) {
		utils.readElementList(ac, () -> ac.getProject().getMicroschemaContainerRoot());
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

		db.asyncTx(() -> {
			Project project = ac.getProject();
			if (!ac.getUser().hasPermission(project, UPDATE_PERM)) {
				String projectUuid = project.getUuid();
				throw error(FORBIDDEN, "error_missing_perm", projectUuid, UPDATE_PERM.getRestPerm().getName());
			}
			MicroschemaContainer microschema = getRootVertex(ac).loadObjectByUuid(ac, microschemaUuid, READ_PERM);
			MicroschemaContainerRoot root = project.getMicroschemaContainerRoot();
			if (root.contains(microschema)) {
				// Microschema has already been assigned. No need to do anything
				return microschema.transformToRest(ac, 0);
			}

			return db.tx(() -> {
				// Assign the microschema to the project
				root.addMicroschema(ac.getUser(), microschema);
				return microschema.transformToRest(ac, 0);
			});
		}).subscribe(model -> ac.send(model, OK), ac::fail);
	}

	public void handleRemoveMicroschemaFromProject(InternalActionContext ac, String microschemaUuid) {
		validateParameter(microschemaUuid, "microschemaUuid");

		db.asyncTx(() -> {
			Project project = ac.getProject();
			String projectUuid = project.getUuid();
			if (!ac.getUser().hasPermission(project, UPDATE_PERM)) {
				throw error(FORBIDDEN, "error_missing_perm", projectUuid, UPDATE_PERM.getRestPerm().getName());
			}
			// TODO check whether microschema is assigned to project
			MicroschemaContainer microschema = getRootVertex(ac).loadObjectByUuid(ac, microschemaUuid, READ_PERM);
			return db.tx(() -> {
				project.getMicroschemaContainerRoot().removeMicroschema(microschema);
				return Single.just(Optional.empty());
			});
		}).subscribe(model -> ac.send(NO_CONTENT), ac::fail);
	}
}
