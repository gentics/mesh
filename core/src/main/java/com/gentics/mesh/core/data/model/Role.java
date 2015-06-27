package com.gentics.mesh.core.data.model;

import java.util.Set;

import com.gentics.mesh.core.data.model.impl.RoleImpl;
import com.gentics.mesh.core.data.model.relationship.Permission;
import com.gentics.mesh.core.rest.role.response.RoleResponse;

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
