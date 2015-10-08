package com.gentics.mesh.core.verticle.project;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.util.VerticleHelper.createObject;
import static com.gentics.mesh.util.VerticleHelper.deleteObject;
import static com.gentics.mesh.util.VerticleHelper.loadTransformAndResponde;
import static com.gentics.mesh.util.VerticleHelper.updateObject;

import org.springframework.stereotype.Component;

import com.gentics.mesh.core.rest.project.ProjectListResponse;
import com.gentics.mesh.core.verticle.handler.AbstractCrudHandler;
import com.gentics.mesh.handler.InternalActionContext;

@Component
public class ProjectCrudHandler extends AbstractCrudHandler {

	@Override
	public void handleCreate(InternalActionContext ac) {
		db.asyncNoTrx(noTrx -> {
			createObject(ac, boot.projectRoot());
		} , rh -> {
			ac.errorHandler().handle(rh);
		});
	}

	@Override
	public void handleDelete(InternalActionContext ac) {
		db.asyncNoTrx(noTrx -> {
			deleteObject(ac, "uuid", "project_deleted", boot.projectRoot());
		} , rh -> {
			ac.errorHandler().handle(rh);
		});
	}

	@Override
	public void handleUpdate(InternalActionContext ac) {
		db.asyncNoTrx(noTrx -> {
			updateObject(ac, "uuid", boot.projectRoot());
		} , rh -> {
			ac.errorHandler().handle(rh);
		});
	}

	@Override
	public void handleRead(InternalActionContext ac) {
		db.asyncNoTrx(noTrx -> {
			loadTransformAndResponde(ac, "uuid", READ_PERM, boot.projectRoot());
		} , rh -> {
			ac.errorHandler().handle(rh);
		});
	}

	@Override
	public void handleReadList(InternalActionContext ac) {
		db.asyncNoTrx(noTrx -> {
			loadTransformAndResponde(ac, boot.projectRoot(), new ProjectListResponse());
		} , rh -> {
			ac.errorHandler().handle(rh);
		});
	}
}
