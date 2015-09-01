package com.gentics.mesh.core.verticle.microschema;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.util.VerticleHelper.deleteObject;
import static com.gentics.mesh.util.VerticleHelper.loadTransformAndResponde;

import org.apache.commons.lang.NotImplementedException;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.rest.schema.MicroschemaListResponse;
import com.gentics.mesh.core.verticle.handler.AbstractCrudHandler;
import com.gentics.mesh.handler.ActionContext;

@Component
public class MicroschemaCrudHandler extends AbstractCrudHandler {

	@Override
	public void handleCreate(ActionContext ac) {
		throw new NotImplementedException();
	}

	@Override
	public void handleDelete(ActionContext ac) {
		deleteObject(ac, "uuid", "group_deleted", boot.microschemaContainerRoot());
	}

	@Override
	public void handleUpdate(ActionContext ac) {
		throw new NotImplementedException();
	}

	@Override
	public void handleRead(ActionContext ac) {
		loadTransformAndResponde(ac, "uuid", READ_PERM, boot.microschemaContainerRoot());
	}

	@Override
	public void handleReadList(ActionContext ac) {
		loadTransformAndResponde(ac, boot.microschemaContainerRoot(), new MicroschemaListResponse());
	}

}
