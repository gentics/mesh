package com.gentics.mesh.core.verticle.schema;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.core.rest.common.GenericMessageResponse.message;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.core.verticle.handler.HandlerUtilities.operateNoTx;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NO_CONTENT;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import javax.inject.Inject;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.data.schema.handler.SchemaComparator;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangesListModel;
import com.gentics.mesh.core.rest.schema.impl.SchemaModel;
import com.gentics.mesh.core.verticle.handler.AbstractCrudHandler;
import com.gentics.mesh.core.verticle.handler.HandlerUtilities;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.json.JsonUtil;

import dagger.Lazy;
import rx.Single;

public class SchemaContainerCrudHandler extends AbstractCrudHandler<SchemaContainer, Schema> {

	private SchemaComparator comparator;

	private Lazy<BootstrapInitializer> boot;

	@Inject
	public SchemaContainerCrudHandler(Database db, SchemaComparator comparator, Lazy<BootstrapInitializer> boot) {
		super(db);
		this.comparator = comparator;
		this.boot = boot;
	}

	@Override
	public RootVertex<SchemaContainer> getRootVertex(InternalActionContext ac) {
		return boot.get().schemaContainerRoot();
	}

	@Override
	public void handleUpdate(InternalActionContext ac, String uuid) {
		validateParameter(uuid, "uuid");
		operateNoTx(ac, () -> {
			RootVertex<SchemaContainer> root = getRootVertex(ac);
			SchemaContainer element = root.loadObjectByUuid(ac, uuid, UPDATE_PERM);
			Schema requestModel = JsonUtil.readValue(ac.getBodyAsString(), SchemaModel.class);
			// Diff the schema with the latest version
			SchemaChangesListModel model = new SchemaChangesListModel();
			model.getChanges().addAll(MeshInternal.get().schemaComparator().diff(element.getLatestVersion().getSchema(), requestModel));
			String schemaName = element.getName();
			// No changes -> done
			if (model.getChanges().isEmpty()) {
				return message(ac, "schema_update_no_difference_detected", schemaName);
			} else {

				db.tx(() -> {
					SearchQueueBatch batch = MeshInternal.get().boot().meshRoot().getSearchQueue().createBatch();
					// Apply the found changes to the schema
					SchemaContainerVersion version = element.getLatestVersion().applyChanges(ac, model, batch);

					//TODO determine whether the newly created schema version should also be directly applied to 
					// ** Parameter: updateAssignedReleases - default true
					// ** Parameter: updateReleaseNames=release1,release2,release3 + Fehler, wenn man eine Release angibt, die noch nicht zugeordnet ist					

					return batch;
				}).processSync();
				return message(ac, "migration_invoked", schemaName);
			}
		}, model -> ac.send(model, OK));
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
		operateNoTx(ac, () -> {
			SchemaContainer schema = getRootVertex(ac).loadObjectByUuid(ac, uuid, READ_PERM);
			Schema requestModel = JsonUtil.readValue(ac.getBodyAsString(), SchemaModel.class);
			return schema.getLatestVersion().diff(ac, comparator, requestModel);
		}, model -> ac.send(model, OK));
	}

	/**
	 * Handle a read project list request.
	 * 
	 * @param ac
	 */
	public void handleReadProjectList(InternalActionContext ac) {
		HandlerUtilities.readElementList(ac, () -> ac.getProject().getSchemaContainerRoot());
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

		operateNoTx(() -> {
			Project project = ac.getProject();
			String projectUuid = project.getUuid();
			if (ac.getUser().hasPermission(project.getImpl(), GraphPermission.UPDATE_PERM)) {
				SchemaContainer schema = getRootVertex(ac).loadObjectByUuid(ac, schemaUuid, READ_PERM);
				return db.tx(() -> {
					// TODO SQB ?
					project.getSchemaContainerRoot().addSchemaContainer(schema);
					return schema.transformToRest(ac, 0);
				});
			} else {
				throw error(FORBIDDEN, "error_missing_perm", projectUuid);
			}

		}).subscribe(model -> ac.send(model, OK), ac::fail);

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

		operateNoTx(() -> {
			Project project = ac.getProject();
			String projectUuid = project.getUuid();
			if (ac.getUser().hasPermission(project.getImpl(), GraphPermission.UPDATE_PERM)) {
				// TODO check whether schema is assigned to project

				SchemaContainer schema = boot.get().schemaContainerRoot().loadObjectByUuid(ac, schemaUuid, READ_PERM);
				return db.tx(() -> {
					project.getSchemaContainerRoot().removeSchemaContainer(schema);
					return Single.just(null);
				});
			} else {
				throw error(FORBIDDEN, "error_missing_perm", projectUuid);
			}

		}).subscribe(model -> ac.send(NO_CONTENT), ac::fail);
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

		operateNoTx(ac, () -> {
			SchemaContainer schema = boot.get().schemaContainerRoot().loadObjectByUuid(ac, schemaUuid, UPDATE_PERM);
			db.tx(() -> {
				SearchQueueBatch batch = MeshInternal.get().boot().meshRoot().getSearchQueue().createBatch();
				schema.getLatestVersion().applyChanges(ac, batch);
				return batch;
			}).processSync();
			return message(ac, "migration_invoked", schema.getName());
		}, model -> ac.send(model, OK));

	}

}
