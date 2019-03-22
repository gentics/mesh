package com.gentics.mesh.core.rest.event.role;

import com.gentics.mesh.ElementType;
import com.gentics.mesh.core.rest.event.AbstractProjectEventModel;

public class PermissionChangedEventModel extends AbstractProjectEventModel {
	private ElementType type;

	public ElementType getType() {
		return type;
	}

	public PermissionChangedEventModel setType(ElementType type) {
		this.type = type;
		return this;
	}
}
