package com.gentics.mesh.core.verticle.project;

import org.springframework.stereotype.Component;

import com.gentics.mesh.core.verticle.handler.AbstractCrudHandler;
import com.gentics.mesh.handler.InternalActionContext;

@Component
public class ProjectCrudHandler extends AbstractCrudHandler {

	@Override
	public void handleCreate(InternalActionContext ac) {
		createElement(ac, () -> boot.projectRoot());
	}

	@Override
	public void handleDelete(InternalActionContext ac) {
		deleteElement(ac, () -> boot.projectRoot(), "uuid", "project_deleted");
	}

	@Override
	public void handleUpdate(InternalActionContext ac) {
		updateElement(ac, "uuid", () -> boot.projectRoot());
	}

	@Override
	public void handleRead(InternalActionContext ac) {
		readElement(ac, "uuid", () -> boot.projectRoot());
	}

	@Override
	public void handleReadList(InternalActionContext ac) {
		readElementList(ac, () -> boot.projectRoot());
	}

}
