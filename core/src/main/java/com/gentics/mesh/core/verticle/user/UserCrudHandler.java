package com.gentics.mesh.core.verticle.user;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.util.VerticleHelper.createObject;
import static com.gentics.mesh.util.VerticleHelper.deleteObject;
import static com.gentics.mesh.util.VerticleHelper.loadObject;
import static com.gentics.mesh.util.VerticleHelper.loadTransformAndResponde;
import static com.gentics.mesh.util.VerticleHelper.updateObject;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import org.springframework.stereotype.Component;

import com.gentics.mesh.core.rest.user.UserListResponse;
import com.gentics.mesh.core.verticle.handler.AbstractCrudHandler;
import com.gentics.mesh.handler.InternalActionContext;

@Component
public class UserCrudHandler extends AbstractCrudHandler {

	@Override
	public void handleDelete(InternalActionContext ac) {
		db.asyncNoTrx(tx -> {
			// TODO invalidate active sessions for this user
			deleteObject(ac, "uuid", "user_deleted", boot.userRoot());
		} , ac.errorHandler());
	}

	@Override
	public void handleCreate(InternalActionContext ac) {
		db.asyncNoTrx(tx -> {
			createObject(ac, boot.userRoot());
		} , ac.errorHandler());
	}

	@Override
	public void handleUpdate(InternalActionContext ac) {
		db.asyncNoTrx(tx -> {
			updateObject(ac, "uuid", boot.userRoot());
		} , ac.errorHandler());
	}

	@Override
	public void handleRead(InternalActionContext ac) {
		db.asyncNoTrx(tx -> {
			loadObject(ac, "uuid", READ_PERM, boot.userRoot(), rh -> {
				loadTransformAndResponde(ac, "uuid", READ_PERM, boot.userRoot(), OK);
			});
		} , ac.errorHandler());
	}

	@Override
	public void handleReadList(InternalActionContext ac) {
		db.asyncNoTrx(tx -> {
			loadTransformAndResponde(ac, boot.userRoot(), new UserListResponse(), OK);
		} , ac.errorHandler());
	}

}
