package com.gentics.mesh.core.verticle.schema;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.util.VerticleHelper.transformAndRespond;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.SchemaContainer;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.verticle.handler.AbstractCrudHandler;
import com.gentics.mesh.handler.InternalActionContext;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;

@Component
public class SchemaContainerCrudHandler extends AbstractCrudHandler<SchemaContainer> {

	@Override
	public RootVertex<SchemaContainer> getRootVertex(InternalActionContext ac) {
		return boot.schemaContainerRoot();
	}

	@Override
	public void handleCreate(InternalActionContext ac) {
		createElement(ac, () -> getRootVertex(ac));
	}

	@Override
	public void handleDelete(InternalActionContext ac) {
		deleteElement(ac, () -> boot.schemaContainerRoot(), "uuid", "schema_deleted");
	}

	@Override
	public void handleUpdate(InternalActionContext ac) {
		updateElement(ac, "uuid", () -> boot.schemaContainerRoot());
	}

	@Override
	public void handleRead(InternalActionContext ac) {
		readElement(ac, "uuid", () -> boot.schemaContainerRoot());
	}

	@Override
	public void handleReadList(InternalActionContext ac) {
		readElementList(ac, () -> boot.schemaContainerRoot());
	}

	public void handleReadProjectList(InternalActionContext ac) {
		readElementList(ac, () -> ac.getProject().getSchemaContainerRoot());
	}

	public void handleAddProjectToSchema(InternalActionContext ac) {
		db.asyncNoTrx(tc -> {
			boot.projectRoot().loadObject(ac, "projectUuid", UPDATE_PERM, rh -> {
				if (ac.failOnError(rh)) {
					boot.schemaContainerRoot().loadObject(ac, "schemaUuid", READ_PERM, srh -> {
						if (ac.failOnError(srh)) {
							Project project = rh.result();
							SchemaContainer schema = srh.result();
							db.trx(addTx -> {
								project.getSchemaContainerRoot().addSchemaContainer(schema);
								addTx.complete(schema);
							} , (AsyncResult<SchemaContainer> rtx) -> {
								if (rtx.failed()) {
									ac.fail(rtx.cause());
								} else {
									transformAndRespond(ac, rtx.result(), OK);
								}
							});
						}
					});
				}
			});
		} , rh -> {
			if (rh.failed()) {
				ac.errorHandler().handle(rh);
			}
		});

	}

	public void handleRemoveProjectFromSchema(InternalActionContext ac) {
		db.asyncNoTrx(tc -> {
			boot.projectRoot().loadObject(ac, "projectUuid", UPDATE_PERM, rh -> {
				if (ac.failOnError(rh)) {
					// TODO check whether schema is assigned to project
					boot.schemaContainerRoot().loadObject(ac, "schemaUuid", READ_PERM, srh -> {
						if (ac.failOnError(srh)) {
							SchemaContainer schema = srh.result();
							Project project = rh.result();
							db.trx(tcRemove -> {
								project.getSchemaContainerRoot().removeSchemaContainer(schema);
								tcRemove.complete(schema);
							} , (AsyncResult<SchemaContainer> schemaRemoved) -> {
								if (schemaRemoved.failed()) {
									ac.errorHandler().handle(Future.failedFuture(schemaRemoved.cause()));
								} else {
									transformAndRespond(ac, schemaRemoved.result(), OK);
								}
							});
						}
					});
				}
			});
		} , rh -> {
			if (rh.failed()) {
				ac.errorHandler().handle(rh);
			}
		});
	}

}
