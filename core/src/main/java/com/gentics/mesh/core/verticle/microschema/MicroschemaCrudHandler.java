package com.gentics.mesh.core.verticle.microschema;

import org.springframework.stereotype.Component;

import com.gentics.mesh.core.verticle.handler.AbstractCrudHandler;
import com.gentics.mesh.handler.InternalActionContext;

@Component
public class MicroschemaCrudHandler extends AbstractCrudHandler {

	@Override
	public void handleCreate(InternalActionContext ac) {
		createElement(ac, () -> boot.microschemaContainerRoot());
	}

	@Override
	public void handleDelete(InternalActionContext ac) {
		deleteElement(ac, () -> boot.microschemaContainerRoot(), "uuid", "microschema_deleted");
	}

	@Override
	public void handleUpdate(InternalActionContext ac) {
		updateElement(ac, "uuid", () -> boot.microschemaContainerRoot());
	}

	@Override
	public void handleRead(InternalActionContext ac) {
		readElement(ac, "uuid", () -> boot.microschemaContainerRoot());
	}

	@Override
	public void handleReadList(InternalActionContext ac) {
		readElementList(ac, () -> boot.microschemaContainerRoot());
	}

}
