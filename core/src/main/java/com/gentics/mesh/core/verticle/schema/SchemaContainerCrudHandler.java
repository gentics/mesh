package com.gentics.mesh.core.verticle.schema;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.schema.handler.SchemaComparator;
import com.gentics.mesh.core.rest.schema.SchemaResponse;
import com.gentics.mesh.core.verticle.handler.AbstractCrudHandler;
import com.gentics.mesh.handler.InternalActionContext;

import rx.Observable;

@Component
public class SchemaContainerCrudHandler extends AbstractCrudHandler<SchemaContainer, SchemaResponse> {

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

	public void handleDiff(InternalActionContext ac) {
		db.asyncNoTrxExperimental(() -> {
			Observable<SchemaContainer> obsSchema = getRootVertex(ac).loadObject(ac, "uuid", READ_PERM);
			return obsSchema.flatMap(schema -> schema.diff(ac, comparator));
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

	public void handleExecuteSchemaChanges(InternalActionContext ac) {
		db.asyncNoTrxExperimental(() -> {
			Observable<SchemaContainer> obsSchema = boot.schemaContainerRoot().loadObject(ac, "schemaUuid", UPDATE_PERM);
			return obsSchema.flatMap(schema -> {
				return schema.getLatestVersion().applyChanges(ac);
			});
		}).subscribe(model -> ac.respond(model, OK), ac::fail);

	}

}
