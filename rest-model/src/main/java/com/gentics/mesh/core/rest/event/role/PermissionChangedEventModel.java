package com.gentics.mesh.core.rest.event.role;

import com.gentics.mesh.ElementType;
import com.gentics.mesh.core.rest.event.MeshElementEventModel;
import com.gentics.mesh.core.rest.role.RoleReference;

public interface PermissionChangedEventModel extends MeshElementEventModel {

	/**
	 * Return the role that was used during the permission change action.
	 * 
	 * @return
	 */
	RoleReference getRole();

	/**
	 * Set the role of the permission change action.
	 * 
	 * @param role
	 * @return Fluent API
	 */
	PermissionChangedEventModel setRole(RoleReference role);

	/**
	 * Return the type of the element for which the permissions changed.
	 * 
	 * @return
	 */
	ElementType getType();

	/**
	 * Set the type of the element for which the permissions have changed.
	 * 
	 * @param type
	 * @return Fluent API
	 */
	PermissionChangedEventModel setType(ElementType type);

}
