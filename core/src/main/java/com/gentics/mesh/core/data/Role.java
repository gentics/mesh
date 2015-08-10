package com.gentics.mesh.core.data;

import java.util.List;
import java.util.Set;

import com.gentics.mesh.core.data.relationship.Permission;
import com.gentics.mesh.core.rest.role.RoleResponse;

public interface Role extends GenericVertex<RoleResponse>, NamedNode {

	public static final String TYPE = "role";

	void grantPermissions(MeshVertex vertex, Permission... permissions);

	void revokePermissions(MeshVertex vertex, Permission... permissions);

	Set<Permission> getPermissions(MeshVertex vertex);

	void addGroup(Group group);

	boolean hasPermission(Permission permission, GenericVertex<?> node);

	List<? extends Group> getGroups();

}
