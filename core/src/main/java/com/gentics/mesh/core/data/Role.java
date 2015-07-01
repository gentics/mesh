package com.gentics.mesh.core.data;

import java.util.Set;

import com.gentics.mesh.core.data.impl.RoleImpl;
import com.gentics.mesh.core.data.relationship.Permission;
import com.gentics.mesh.core.rest.role.RoleResponse;

public interface Role extends GenericNode {

	void addPermissions(MeshVertex vertex, Permission... permissions);

	void revokePermissions(MeshVertex vertex, Permission... permissions);

	String getName();

	Set<Permission> getPermissions(MeshVertex vertex);

	void setName(String name);

	RoleResponse transformToRest();

	RoleImpl getImpl();

	void addGroup(Group group);

	void delete();

}
