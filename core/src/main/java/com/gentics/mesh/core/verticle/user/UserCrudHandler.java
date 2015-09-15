package com.gentics.mesh.core.verticle.user;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.util.VerticleHelper.createObject;
import static com.gentics.mesh.util.VerticleHelper.deleteObject;
import static com.gentics.mesh.util.VerticleHelper.loadObject;
import static com.gentics.mesh.util.VerticleHelper.loadTransformAndResponde;
import static com.gentics.mesh.util.VerticleHelper.updateObject;

import org.springframework.stereotype.Component;

import com.gentics.mesh.core.rest.user.UserListResponse;
import com.gentics.mesh.core.verticle.handler.AbstractCrudHandler;
import com.gentics.mesh.graphdb.NoTrx;
import com.gentics.mesh.handler.ActionContext;

@Component
public class UserCrudHandler extends AbstractCrudHandler {

	@Override
	public void handleDelete(ActionContext ac) {
		try (NoTrx tx = db.noTrx()) {
			deleteObject(ac, "uuid", "user_deleted", boot.userRoot());
		}
	}

	@Override
	public void handleCreate(ActionContext ac) {
		try (NoTrx tx = db.noTrx()) {
			createObject(ac, boot.userRoot());
		}
	}

	@Override
	public void handleUpdate(ActionContext ac) {
		try (NoTrx tx = db.noTrx()) {
			updateObject(ac, "uuid", boot.userRoot());
		}
	}

	@Override
	public void handleRead(ActionContext ac) {
		try (NoTrx tx = db.noTrx()) {
			loadObject(ac, "uuid", READ_PERM, boot.userRoot(), rh -> {
				loadTransformAndResponde(ac, "uuid", READ_PERM, boot.userRoot());
			});
		}
	}

	@Override
	public void handleReadList(ActionContext ac) {
		try (NoTrx tx = db.noTrx()) {
			loadTransformAndResponde(ac, boot.userRoot(), new UserListResponse());
		}
	}

}
