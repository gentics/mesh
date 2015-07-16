package com.gentics.mesh.core.data;

import java.util.List;
import java.util.Set;

import com.gentics.mesh.core.data.impl.RoleImpl;
import com.gentics.mesh.core.data.relationship.Permission;
import com.gentics.mesh.core.rest.role.RoleResponse;

public interface Role extends GenericVertex<RoleResponse>, NamedNode {

	void addPermissions(MeshVertex vertex, Permission... permissions);

	void revokePermissions(MeshVertex vertex, Permission... permissions);

	Set<Permission> getPermissions(MeshVertex vertex);

	RoleImpl getImpl();

	void addGroup(Group group);

	boolean hasPermission(Permission permission, GenericVertex<?> node);

	List<? extends Group> getGroups();

}
