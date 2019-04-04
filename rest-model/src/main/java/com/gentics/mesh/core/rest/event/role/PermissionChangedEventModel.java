package com.gentics.mesh.core.rest.event.role;

import com.gentics.mesh.ElementType;
import com.gentics.mesh.core.rest.event.AbstractElementMeshEventModel;
import com.gentics.mesh.core.rest.project.ProjectReference;
import com.gentics.mesh.core.rest.role.RoleReference;

public class PermissionChangedEventModel extends AbstractElementMeshEventModel {

	private ElementType type;

	private RoleReference role;

	private ProjectReference project;

	public RoleReference getRole() {
		return role;
	}

	public PermissionChangedEventModel setRole(RoleReference role) {
		this.role = role;
		return this;
	}

	public ElementType getType() {
		return type;
	}

	public PermissionChangedEventModel setType(ElementType type) {
		this.type = type;
		return this;

	}

	public ProjectReference getProject() {
		return project;
	}

	public PermissionChangedEventModel setProject(ProjectReference project) {
		this.project = project;
		return this;
	}

}
