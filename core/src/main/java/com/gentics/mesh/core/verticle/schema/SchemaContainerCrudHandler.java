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

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.SchemaContainer;
import com.gentics.mesh.core.rest.schema.SchemaListResponse;
import com.gentics.mesh.core.verticle.handler.AbstractCrudHandler;
import com.gentics.mesh.graphdb.Trx;

import io.vertx.ext.web.RoutingContext;

@Component
public class SchemaContainerCrudHandler extends AbstractCrudHandler {

	@Override
	public void handleCreate(RoutingContext rc) {
		try (Trx tx = new Trx(db)) {
			createObject(rc, boot.schemaContainerRoot());
		}

	}

	@Override
	public void handleDelete(RoutingContext rc) {
		try (Trx tx = new Trx(db)) {
			deleteObject(rc, "uuid", "schema_deleted", boot.schemaContainerRoot());
		}
	}

	@Override
	public void handleUpdate(RoutingContext rc) {
		try (Trx tx = new Trx(db)) {
			updateObject(rc, "uuid", boot.schemaContainerRoot());
		}
	}

	@Override
	public void handleRead(RoutingContext rc) {
		String uuid = rc.request().params().get("uuid");
		if (StringUtils.isEmpty(uuid)) {
			rc.next();
		} else {
			try (Trx tx = new Trx(db)) {
				loadTransformAndResponde(rc, "uuid", READ_PERM, boot.schemaContainerRoot());
			}
		}
	}

	@Override
	public void handleReadList(RoutingContext rc) {
		try (Trx tx = new Trx(db)) {
			loadTransformAndResponde(rc, boot.schemaContainerRoot(), new SchemaListResponse());
		}
	}

	public void handleAddProjectToSchema(RoutingContext rc) {
		try (Trx tx = new Trx(db)) {
			loadObject(rc, "projectUuid", UPDATE_PERM, boot.projectRoot(), rh -> {
				if (hasSucceeded(rc, rh)) {
					loadObject(rc, "schemaUuid", READ_PERM, boot.schemaContainerRoot(), srh -> {
						if (hasSucceeded(rc, srh)) {
							Project project = rh.result();
							SchemaContainer schema = srh.result();
							try (Trx txAdd = new Trx(db)) {
								project.getSchemaContainerRoot().addSchemaContainer(schema);
								txAdd.success();
							}
							// TODO add simple message or return schema?
							transformAndResponde(rc, schema);
						}
					});
				}
			});
		}

	}

	public void handleRemoveProjectFromSchema(RoutingContext rc) {
		try (Trx tx = new Trx(db)) {
			loadObject(rc, "projectUuid", UPDATE_PERM, boot.projectRoot(), rh -> {
				if (hasSucceeded(rc, rh)) {
					// TODO check whether schema is assigned to project
					loadObject(rc, "schemaUuid", READ_PERM, boot.schemaContainerRoot(), srh -> {
						if (hasSucceeded(rc, srh)) {
							SchemaContainer schema = srh.result();
							Project project = rh.result();
							try (Trx txRemove = new Trx(db)) {
								project.getSchemaContainerRoot().removeSchemaContainer(schema);
								txRemove.success();
							}
							transformAndResponde(rc, schema);
						}
					});
				}
			});
		}
	}

}
