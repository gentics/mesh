package com.gentics.mesh.core.verticle.microschema;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.util.VerticleHelper.loadTransformAndRespond;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import org.springframework.stereotype.Component;

import com.gentics.mesh.core.rest.schema.MicroschemaListResponse;
import com.gentics.mesh.core.verticle.handler.AbstractCrudHandler;
import com.gentics.mesh.handler.InternalActionContext;

@Component
public class MicroschemaCrudHandler extends AbstractCrudHandler {

	@Override
	public void handleCreate(InternalActionContext ac) {
		db.asyncNoTrx(tc -> {
			createObject(ac, boot.microschemaContainerRoot());
		} , rh -> {
			if (rh.failed()) {
				ac.errorHandler().handle(rh);
			}
		});
	}

	@Override
	public void handleDelete(InternalActionContext ac) {
		db.asyncNoTrx(tc -> {
			deleteObject(ac, "uuid", "microschema_deleted", boot.microschemaContainerRoot());
		} , rh -> {
			if (rh.failed()) {
				ac.errorHandler().handle(rh);
			}
		});
	}

	@Override
	public void handleUpdate(InternalActionContext ac) {
		db.asyncNoTrx(tc -> {
			updateObject(ac, "uuid", boot.microschemaContainerRoot());
		} , rh -> {
			if (rh.failed()) {
				ac.errorHandler().handle(rh);
			}
		});
	}

	@Override
	public void handleRead(InternalActionContext ac) {
		db.asyncNoTrx(tc -> {
			loadTransformAndRespond(ac, "uuid", READ_PERM, boot.microschemaContainerRoot(), OK);
		} , rh -> {
			if (rh.failed()) {
				ac.errorHandler().handle(rh);
			}
		});
	}

	@Override
	public void handleReadList(InternalActionContext ac) {
		db.asyncNoTrx(tc -> {
			loadTransformAndRespond(ac, boot.microschemaContainerRoot(), new MicroschemaListResponse(), OK);
		} , rh -> {
			if (rh.failed()) {
				ac.errorHandler().handle(rh);
			}
		});
	}

}
