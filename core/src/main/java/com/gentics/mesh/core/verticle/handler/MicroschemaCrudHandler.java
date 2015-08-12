package com.gentics.mesh.core.verticle.handler;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.util.VerticleHelper.loadTransformAndResponde;
import io.vertx.ext.web.RoutingContext;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.rest.schema.MicroschemaListResponse;

@Component
public class MicroschemaCrudHandler extends AbstractCrudHandler {

	@Override
	public void handleCreate(RoutingContext rc) {
		// TODO Auto-generated method stub

	}

	@Override
	public void handleDelete(RoutingContext rc) {
		delete(rc, "uuid", "group_deleted", boot.microschemaContainerRoot());
	}

	@Override
	public void handleUpdate(RoutingContext rc) {
		// TODO Auto-generated method stub

	}

	@Override
	public void handleRead(RoutingContext rc) {
		String uuid = rc.request().params().get("uuid");
		if (StringUtils.isEmpty(uuid)) {
			rc.next();
		} else {
			loadTransformAndResponde(rc, "uuid", READ_PERM, boot.microschemaContainerRoot());
		}
	}

	@Override
	public void handleReadList(RoutingContext rc) {
		loadTransformAndResponde(rc, boot.microschemaContainerRoot(), new MicroschemaListResponse());
	}

}
