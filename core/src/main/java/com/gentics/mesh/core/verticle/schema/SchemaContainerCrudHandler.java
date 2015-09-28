package com.gentics.mesh.core.verticle.schema;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.util.VerticleHelper.createObject;
import static com.gentics.mesh.util.VerticleHelper.deleteObject;
import static com.gentics.mesh.util.VerticleHelper.hasSucceeded;
import static com.gentics.mesh.util.VerticleHelper.loadObject;
import static com.gentics.mesh.util.VerticleHelper.loadTransformAndResponde;
import static com.gentics.mesh.util.VerticleHelper.transformAndResponde;
import static com.gentics.mesh.util.VerticleHelper.updateObject;

import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.SchemaContainer;
import com.gentics.mesh.core.rest.schema.SchemaListResponse;
import com.gentics.mesh.core.verticle.handler.AbstractCrudHandler;
import com.gentics.mesh.handler.InternalActionContext;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;

@Component
public class SchemaContainerCrudHandler extends AbstractCrudHandler {

	@Override
	public void handleCreate(InternalActionContext ac) {
		db.asyncNoTrx(tc -> {
			createObject(ac, boot.schemaContainerRoot());
		} , rh -> {
			if (rh.failed()) {
				ac.errorHandler().handle(rh);
			}
		});

	}

	@Override
	public void handleDelete(InternalActionContext ac) {
		db.asyncNoTrx(tc -> {
			deleteObject(ac, "uuid", "schema_deleted", boot.schemaContainerRoot());
		} , rh -> {
			if (rh.failed()) {
				ac.errorHandler().handle(rh);
			}
		});

	}

	@Override
	public void handleUpdate(InternalActionContext ac) {
		db.asyncNoTrx(tc -> {
			updateObject(ac, "uuid", boot.schemaContainerRoot());
		} , rh -> {
			if (rh.failed()) {
				ac.errorHandler().handle(rh);
			}
		});

	}

	@Override
	public void handleRead(InternalActionContext ac) {
		db.asyncNoTrx(tc -> {
			loadTransformAndResponde(ac, "uuid", READ_PERM, boot.schemaContainerRoot());
		} , rh -> {
			if (rh.failed()) {
				ac.errorHandler().handle(rh);
			}
		});
	}

	@Override
	public void handleReadList(InternalActionContext ac) {
		db.asyncNoTrx(tc -> {
			loadTransformAndResponde(ac, boot.schemaContainerRoot(), new SchemaListResponse());
		} , rh -> {
			if (rh.failed()) {
				ac.errorHandler().handle(rh);
			}
		});
	}

	public void handleAddProjectToSchema(InternalActionContext ac) {
		db.asyncNoTrx(tc -> {
			loadObject(ac, "projectUuid", UPDATE_PERM, boot.projectRoot(), rh -> {
				if (hasSucceeded(ac, rh)) {
					loadObject(ac, "schemaUuid", READ_PERM, boot.schemaContainerRoot(), srh -> {
						if (hasSucceeded(ac, srh)) {
							Project project = rh.result();
							SchemaContainer schema = srh.result();
							db.blockingTrx(addTx -> {
								project.getSchemaContainerRoot().addSchemaContainer(schema);
								addTx.complete(schema);
							} , (AsyncResult<SchemaContainer> rtx) -> {
								if (rtx.failed()) {
									ac.fail(rtx.cause());
								} else {
									transformAndResponde(ac, rtx.result());
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
			loadObject(ac, "projectUuid", UPDATE_PERM, boot.projectRoot(), rh -> {
				if (hasSucceeded(ac, rh)) {
					// TODO check whether schema is assigned to project
					loadObject(ac, "schemaUuid", READ_PERM, boot.schemaContainerRoot(), srh -> {
						if (hasSucceeded(ac, srh)) {
							SchemaContainer schema = srh.result();
							Project project = rh.result();
							db.blockingTrx(tcRemove -> {
								project.getSchemaContainerRoot().removeSchemaContainer(schema);
								tcRemove.complete(schema);
							} , (AsyncResult<SchemaContainer> schemaRemoved) -> {
								if (schemaRemoved.failed()) {
									ac.errorHandler().handle(Future.failedFuture(schemaRemoved.cause()));
								} else {
									transformAndResponde(ac, schemaRemoved.result());
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
