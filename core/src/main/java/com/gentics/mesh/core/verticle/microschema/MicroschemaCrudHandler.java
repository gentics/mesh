package com.gentics.mesh.core.verticle.microschema;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.core.rest.common.GenericMessageResponse.message;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import javax.inject.Inject;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.data.schema.handler.MicroschemaComparator;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaModel;
import com.gentics.mesh.core.rest.schema.Microschema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangesListModel;
import com.gentics.mesh.core.verticle.handler.AbstractCrudHandler;
import com.gentics.mesh.core.verticle.handler.HandlerUtilities;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.json.JsonUtil;

import dagger.Lazy;
import rx.Single;

public class MicroschemaCrudHandler extends AbstractCrudHandler<MicroschemaContainer, Microschema> {

	private MicroschemaComparator comparator;

	private Lazy<BootstrapInitializer> boot;

	@Inject
	public MicroschemaCrudHandler(Database db, MicroschemaComparator comparator, Lazy<BootstrapInitializer> boot) {
		super(db);
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

		db.asyncNoTx(() -> {
			RootVertex<MicroschemaContainer> root = getRootVertex(ac);
			return root.loadObjectByUuid(ac, uuid, UPDATE_PERM).flatMap(element -> {
				return db.tx(() -> {
					try {
						Microschema requestModel = JsonUtil.readValue(ac.getBodyAsString(), MicroschemaModel.class);
						SchemaChangesListModel model = new SchemaChangesListModel();
						model.getChanges()
								.addAll(MeshInternal.get().microschemaComparator().diff(element.getLatestVersion().getSchema(), requestModel));
						String name = element.getName();
						if (model.getChanges().isEmpty()) {
							return Single.just(message(ac, "schema_update_no_difference_detected", name));
						} else {
							return element.getLatestVersion().applyChanges(ac, model).flatMap(e -> {
								return Single.just(message(ac, "migration_invoked", name));
							});
						}
					} catch (Exception e) {
						return Single.error(e);
					}
				});
			});
		}).subscribe(model -> ac.send(model, OK), ac::fail);

	}

	@Override
	public void handleDelete(InternalActionContext ac, String uuid) {
		HandlerUtilities.deleteElement(ac, () -> getRootVertex(ac), uuid);
	}

	/**
	 * Compare the latest schema version with the given schema model.
	 * 
	 * @param uuid
	 *            Schema uuid
	 * @param ac
	 */
	public void handleDiff(InternalActionContext ac, String uuid) {
		db.asyncNoTx(() -> {
			Single<MicroschemaContainer> obsSchema = getRootVertex(ac).loadObjectByUuid(ac, uuid, READ_PERM);
			Microschema requestModel = JsonUtil.readValue(ac.getBodyAsString(), MicroschemaModel.class);
			return obsSchema.flatMap(microschema -> microschema.getLatestVersion().diff(ac, comparator, requestModel));
		}).subscribe(model -> ac.send(model, OK), ac::fail);
	}

	public void handleGetSchemaChanges(InternalActionContext ac, String schemaUuid) {
		// TODO Auto-generated method stub

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
		db.asyncNoTx(() -> {
			Single<MicroschemaContainer> obsSchema = boot.get().microschemaContainerRoot().loadObjectByUuid(ac, schemaUuid, UPDATE_PERM);
			return obsSchema.flatMap(schema -> {
				return schema.getLatestVersion().applyChanges(ac);
			});
		}).subscribe(model -> ac.send(model, OK), ac::fail);

	}

	/**
	 * Handle a microschema read list request.
	 * 
	 * @param ac
	 */
	public void handleReadMicroschemaList(InternalActionContext ac) {
		HandlerUtilities.readElementList(ac, () -> ac.getProject().getMicroschemaContainerRoot());
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

		db.asyncNoTx(() -> {
			Project project = ac.getProject();
			if (ac.getUser().hasPermission(project.getImpl(), UPDATE_PERM)) {
				return getRootVertex(ac).loadObjectByUuid(ac, microschemaUuid, READ_PERM).flatMap(microschema -> {
					return db.tx(() -> {
						project.getMicroschemaContainerRoot().addMicroschema(microschema);
						return microschema.transformToRest(ac, 0);
					});
				});
			} else {
				String projectUuid = project.getUuid();
				throw error(FORBIDDEN, "error_missing_perm", projectUuid);
			}
		}).subscribe(model -> ac.send(model, OK), ac::fail);
	}

	public void handleRemoveMicroschemaFromProject(InternalActionContext ac, String microschemaUuid) {
		validateParameter(microschemaUuid, "microschemaUuid");

		db.asyncNoTx(() -> {
			Project project = ac.getProject();
			String projectUuid = project.getUuid();
			if(ac.getUser().hasPermission(project.getImpl(), UPDATE_PERM)) {
//				TODO check whether microschema is assigned to project
				return getRootVertex(ac).loadObjectByUuid(ac, microschemaUuid, READ_PERM).flatMap(microschema -> { 
					return db.tx(() -> {
						project.getMicroschemaContainerRoot().removeMicroschema(microschema);
						return Single.just(null);
					});
				});
			} else {
				throw error(FORBIDDEN, "error_missing_perm", projectUuid);
			}
		});
			// 		}).subscribe(model -> ac.send(NO_CONTENT), ac::fail);
	}
}
