package com.gentics.mesh.core.rest.event.role;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.ElementType;
import com.gentics.mesh.core.rest.event.AbstractElementMeshEventModel;
import com.gentics.mesh.core.rest.role.RoleReference;

public class PermissionChangedEventModelImpl extends AbstractElementMeshEventModel implements PermissionChangedEventModel {

	@JsonProperty(required = true)
	@JsonPropertyDescription("Type of the element for which the permissions have changed.")
	private ElementType type;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Reference to the role that was used when changing permissions.")
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
