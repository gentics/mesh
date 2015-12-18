package com.gentics.mesh.core.verticle.microschema;

import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.MicroschemaContainer;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.verticle.handler.AbstractCrudHandler;
import com.gentics.mesh.handler.InternalActionContext;

@Component
public class MicroschemaCrudHandler extends AbstractCrudHandler<MicroschemaContainer> {

	@Override
	public RootVertex<MicroschemaContainer> getRootVertex(InternalActionContext ac) {
		return boot.microschemaContainerRoot();
	}

	@Override
	public void handleCreate(InternalActionContext ac) {
		createElement(ac, () -> getRootVertex(ac));
	}

	@Override
	public void handleDelete(InternalActionContext ac) {
		deleteElement(ac, () -> getRootVertex(ac), "uuid", "microschema_deleted");
	}

	@Override
	public void handleUpdate(InternalActionContext ac) {
		updateElement(ac, "uuid", () -> getRootVertex(ac));
	}

	@Override
	public void handleRead(InternalActionContext ac) {
		readElement(ac, "uuid", () -> getRootVertex(ac));
	}

	@Override
	public void handleReadList(InternalActionContext ac) {
		readElementList(ac, () -> getRootVertex(ac));
	}

}
