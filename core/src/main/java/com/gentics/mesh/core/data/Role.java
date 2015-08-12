package com.gentics.mesh.core.data;

import java.util.List;
import java.util.Set;

import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.rest.role.RoleResponse;

public interface Role extends GenericVertex<RoleResponse>, NamedNode {

	public static final String TYPE = "role";

	void grantPermissions(MeshVertex vertex, GraphPermission... permissions);

	void revokePermissions(MeshVertex vertex, GraphPermission... permissions);

	Set<GraphPermission> getPermissions(MeshVertex vertex);

	void addGroup(Group group);

	boolean hasPermission(GraphPermission permission, GenericVertex<?> node);

	List<? extends Group> getGroups();

}
