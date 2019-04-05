package com.gentics.mesh.core.rest.event.role;

import com.gentics.mesh.ElementType;
import com.gentics.mesh.core.rest.event.MeshElementEventModel;
import com.gentics.mesh.core.rest.role.RoleReference;

public interface PermissionChangedEventModel extends MeshElementEventModel {

	RoleReference getRole();

	PermissionChangedEventModel setRole(RoleReference role);

	ElementType getType();

	PermissionChangedEventModel setType(ElementType type);

}
