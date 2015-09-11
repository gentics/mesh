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
import com.gentics.mesh.graphdb.Trx;
import com.gentics.mesh.handler.ActionContext;

@Component
public class SchemaContainerCrudHandler extends AbstractCrudHandler {

	@Override
	public void handleCreate(ActionContext ac) {
		try (Trx tx = db.trx()) {
			createObject(ac, boot.schemaContainerRoot());
		}
	}

	@Override
	public void handleDelete(ActionContext ac) {
		try (Trx tx = db.trx()) {
			deleteObject(ac, "uuid", "schema_deleted", boot.schemaContainerRoot());
		}
	}

	@Override
	public void handleUpdate(ActionContext ac) {
		try (Trx tx = db.trx()) {
			updateObject(ac, "uuid", boot.schemaContainerRoot());
		}
	}

	@Override
	public void handleRead(ActionContext ac) {
		try (Trx tx = db.trx()) {
			loadTransformAndResponde(ac, "uuid", READ_PERM, boot.schemaContainerRoot());
		}
	}

	@Override
	public void handleReadList(ActionContext ac) {
		try (Trx tx = db.trx()) {
			loadTransformAndResponde(ac, boot.schemaContainerRoot(), new SchemaListResponse());
		}
	}

	public void handleAddProjectToSchema(ActionContext ac) {
		try (Trx tx = db.trx()) {
			loadObject(ac, "projectUuid", UPDATE_PERM, boot.projectRoot(), rh -> {
				if (hasSucceeded(ac, rh)) {
					loadObject(ac, "schemaUuid", READ_PERM, boot.schemaContainerRoot(), srh -> {
						if (hasSucceeded(ac, srh)) {
							Project project = rh.result();
							SchemaContainer schema = srh.result();
							try (Trx txAdd = db.trx()) {
								project.getSchemaContainerRoot().addSchemaContainer(schema);
								txAdd.success();
							}
							transformAndResponde(ac, schema);
						}
					});
				}
			});
		}

	}

	public void handleRemoveProjectFromSchema(ActionContext ac) {
		try (Trx tx = db.trx()) {
			loadObject(ac, "projectUuid", UPDATE_PERM, boot.projectRoot(), rh -> {
				if (hasSucceeded(ac, rh)) {
					// TODO check whether schema is assigned to project
					loadObject(ac, "schemaUuid", READ_PERM, boot.schemaContainerRoot(), srh -> {
						if (hasSucceeded(ac, srh)) {
							SchemaContainer schema = srh.result();
							Project project = rh.result();
							try (Trx txRemove = db.trx()) {
								project.getSchemaContainerRoot().removeSchemaContainer(schema);
								txRemove.success();
							}
							transformAndResponde(ac, schema);
						}
					});
				}
			});
		}
	}

}
