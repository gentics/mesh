package com.gentics.mesh.core.rest.event.role;

import com.gentics.mesh.ElementType;
import com.gentics.mesh.core.rest.event.AbstractElementMeshEventModel;
import com.gentics.mesh.core.rest.role.RoleReference;

public class PermissionChangedEventModelImpl extends AbstractElementMeshEventModel implements PermissionChangedEventModel {

	private ElementType type;

	private RoleReference role;

	@Override
	public RoleReference getRole() {
		return role;
	}

	@Override
	public PermissionChangedEventModel setRole(RoleReference role) {
		this.role = role;
		return this;
	}

	@Override
	public ElementType getType() {
		return type;
	}

	@Override
	public PermissionChangedEventModel setType(ElementType type) {
		this.type = type;
		return this;
	}

}
