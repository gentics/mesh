package com.gentics.mesh.core.data.model.tinkerpop;

import java.util.List;

import com.gentics.mesh.core.data.model.auth.AuthRelationships;
import com.gentics.mesh.core.data.model.generic.GenericNode;
import com.gentics.mesh.core.data.model.generic.MeshVertex;

public class Role extends GenericNode {

	//TODO index on name
	public String getName() {
		return getProperty("name");
	}

	public void setName(String name) {
		setProperty("name", name);
	}

	public List<? extends GraphPermission> getPermissions() {
		return outE(AuthRelationships.HAS_PERMISSION).toList(GraphPermission.class);
	}

	//	@Adjacency(label = AuthRelationships.HAS_ROLE, direction = Direction.OUT)
	public List<? extends Group> getGroups() {
		return out(AuthRelationships.HAS_ROLE).toList(Group.class);
	}

	public void addGroup(Group group) {
		linkOut(group, AuthRelationships.HAS_ROLE);
	}

	public GraphPermission addPermission(MeshVertex node) {
		return addFramedEdge(AuthRelationships.HAS_PERMISSION, node, GraphPermission.class);
	}

}
