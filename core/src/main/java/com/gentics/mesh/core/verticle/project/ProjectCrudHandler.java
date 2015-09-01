package com.gentics.mesh.core.verticle.project;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.util.VerticleHelper.createObject;
import static com.gentics.mesh.util.VerticleHelper.deleteObject;
import static com.gentics.mesh.util.VerticleHelper.loadTransformAndResponde;
import static com.gentics.mesh.util.VerticleHelper.updateObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.rest.project.ProjectListResponse;
import com.gentics.mesh.core.verticle.handler.AbstractCrudHandler;
import com.gentics.mesh.graphdb.Trx;
import com.gentics.mesh.handler.ActionContext;

@Component
public class ProjectCrudHandler extends AbstractCrudHandler {

	private static final Logger log = LoggerFactory.getLogger(ProjectVerticle.class);

	@Override
	public void handleCreate(ActionContext ac) {
		try (Trx tx = db.trx()) {
			createObject(ac, boot.projectRoot());
		}
	}

	@Override
	public void handleDelete(ActionContext ac) {
		try (Trx tx = db.trx()) {
			deleteObject(ac, "uuid", "project_deleted", boot.projectRoot());
		}
	}

	@Override
	public void handleUpdate(ActionContext ac) {
		try (Trx tx = db.trx()) {
			updateObject(ac, "uuid", boot.projectRoot());
		}
	}

	@Override
	public void handleRead(ActionContext ac) {
		try (Trx tx = db.trx()) {
			loadTransformAndResponde(ac, "uuid", READ_PERM, boot.projectRoot());
		}
	}

	@Override
	public void handleReadList(ActionContext ac) {
		try (Trx tx = db.trx()) {
			loadTransformAndResponde(ac, boot.projectRoot(), new ProjectListResponse());
		}
	}
}
