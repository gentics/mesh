package com.gentics.mesh.core.rest.event.role;

import com.gentics.mesh.ElementType;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.event.AbstractProjectEventModel;
import com.gentics.mesh.core.rest.event.EventCauseInfo;
import com.gentics.mesh.core.rest.project.ProjectReference;

public class PermissionChangedEventModel extends AbstractProjectEventModel {
	private ElementType type;

	public ElementType getType() {
		return type;
	}

	public PermissionChangedEventModel setType(ElementType type) {
		this.type = type;
		return this;
	}

	public PermissionChangedEventModel(String origin, EventCauseInfo cause, MeshEvent event, String uuid, String name, ProjectReference project) {
		super(origin, cause, event, uuid, name, project);
	}

}
