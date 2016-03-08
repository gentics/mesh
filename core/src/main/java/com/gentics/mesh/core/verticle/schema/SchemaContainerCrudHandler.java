package com.gentics.mesh.core.verticle.schema;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.core.rest.common.GenericMessageResponse.message;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.schema.handler.SchemaComparator;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangesListModel;
import com.gentics.mesh.core.rest.schema.impl.SchemaModel;
import com.gentics.mesh.core.verticle.handler.AbstractCrudHandler;
import com.gentics.mesh.handler.InternalActionContext;
import com.gentics.mesh.json.JsonUtil;

import rx.Observable;

@Component
public class SchemaContainerCrudHandler extends AbstractCrudHandler<SchemaContainer, Schema> {

	@Autowired
	private SchemaComparator comparator;

	@Override
	public RootVertex<SchemaContainer> getRootVertex(InternalActionContext ac) {
		return boot.schemaContainerRoot();
	}

	@Override
	public void handleDelete(InternalActionContext ac) {
		deleteElement(ac, () -> boot.schemaContainerRoot(), "uuid", "schema_deleted");
	}

	@Override
	public void handleUpdate(InternalActionContext ac) {
		db.asyncNoTrxExperimental(() -> {
			RootVertex<SchemaContainer> root = getRootVertex(ac);
			return root.loadObject(ac, "uuid", UPDATE_PERM).flatMap(element -> {
				try {
					Schema requestModel = JsonUtil.readSchema(ac.getBodyAsString(), SchemaModel.class);
					SchemaChangesListModel model = new SchemaChangesListModel();
					model.getChanges().addAll(SchemaComparator.getIntance().diff(element.getLatestVersion().getSchema(), requestModel));
					String schemaName = element.getName();
					if (model.getChanges().isEmpty()) {
						return Observable.just(message(ac, "schema_update_no_difference_detected", schemaName));
					} else {
						return element.getLatestVersion().applyChanges(ac, model).flatMap(e -> {
							return Observable.just(message(ac, "migration_invoked", schemaName));
						});
					}
				} catch (Exception e) {
					return Observable.error(e);
				}
			});
		}).subscribe(model -> ac.respond(model, OK), ac::fail);
	}

	public void handleDiff(InternalActionContext ac) {
		db.asyncNoTrxExperimental(() -> {
			Observable<SchemaContainer> obsSchema = getRootVertex(ac).loadObject(ac, "uuid", READ_PERM);
			Schema requestModel = JsonUtil.readSchema(ac.getBodyAsString(), SchemaModel.class);
			return obsSchema.flatMap(schema -> schema.getLatestVersion().diff(ac, comparator, requestModel));
		}).subscribe(model -> ac.respond(model, OK), ac::fail);
	}

	public void handleReadProjectList(InternalActionContext ac) {
		readElementList(ac, () -> ac.getProject().getSchemaContainerRoot());
	}

	public void handleAddProjectToSchema(InternalActionContext ac) {
		db.asyncNoTrxExperimental(() -> {

			Observable<SchemaContainer> obsSchema = getRootVertex(ac).loadObject(ac, "schemaUuid", READ_PERM);
			Observable<Project> obsProject = boot.projectRoot().loadObject(ac, "projectUuid", UPDATE_PERM);

			return Observable.zip(obsProject, obsSchema, (project, schema) -> {
				SchemaContainer addedSchema = db.trx(() -> {
					//TODO SQB ?
					project.getSchemaContainerRoot().addSchemaContainer(schema);
					return schema;
				});
				return addedSchema.transformToRest(ac);
			}).flatMap(x -> x);

		}).subscribe(model -> ac.respond(model, OK), ac::fail);

	}

	public void handleRemoveProjectFromSchema(InternalActionContext ac) {
		db.asyncNoTrxExperimental(() -> {
			Observable<Project> obsProject = boot.projectRoot().loadObject(ac, "projectUuid", UPDATE_PERM);
			// TODO check whether schema is assigned to project
			Observable<SchemaContainer> obsSchema = boot.schemaContainerRoot().loadObject(ac, "schemaUuid", READ_PERM);

			return Observable.zip(obsProject, obsSchema, (project, schema) -> {
				SchemaContainer removedSchema = db.trx(() -> {
					project.getSchemaContainerRoot().removeSchemaContainer(schema);
					return schema;
				});
				return removedSchema.transformToRest(ac);
			}).flatMap(x -> x);
		}).subscribe(model -> ac.respond(model, OK), ac::fail);
	}

	public void handleGetSchemaChanges(InternalActionContext ac) {
		// TODO Auto-generated method stub

	}

	public void handleApplySchemaChanges(InternalActionContext ac) {
		db.asyncNoTrxExperimental(() -> {
			Observable<SchemaContainer> obsSchema = boot.schemaContainerRoot().loadObject(ac, "schemaUuid", UPDATE_PERM);
			return obsSchema.flatMap(schema -> {
				return schema.getLatestVersion().applyChanges(ac);
			});
		}).subscribe(model -> ac.respond(model, OK), ac::fail);

	}

}
