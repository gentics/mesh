package com.gentics.mesh.core.verticle.project;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.util.VerticleHelper.loadTransformAndRespond;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

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
			loadTransformAndRespond(ac, "uuid", READ_PERM, boot.projectRoot(), OK);
		} , rh -> {
			ac.errorHandler().handle(rh);
		});
	}

	@Override
	public void handleReadList(InternalActionContext ac) {
		db.asyncNoTrx(noTrx -> {
			loadTransformAndRespond(ac, boot.projectRoot(), new ProjectListResponse(), OK);
		} , rh -> {
			ac.errorHandler().handle(rh);
		});
	}
}
