package com.gentics.mesh.core.data.model.tinkerpop;

import java.util.List;

import com.gentics.mesh.core.data.model.auth.AuthRelationships;
import com.gentics.mesh.core.data.model.generic.GenericNode;

public class Role extends GenericNode {

	//TODO index on name
	public String getName() {
		return getProperty("name");
	}

	public void setName(String name) {
		setProperty("name", name);
	}

	//	@Adjacency(label = AuthRelationships.HAS_PERMISSION, direction = Direction.OUT)
	public List<GraphPermission> getPermissions() {
		return out(AuthRelationships.HAS_PERMISSION).toList(GraphPermission.class);
	}

	//	@Adjacency(label = AuthRelationships.HAS_PERMISSION, direction = Direction.OUT)
	public void addPermission(GraphPermission permission);

	//	@Adjacency(label = AuthRelationships.HAS_ROLE, direction = Direction.OUT)
	public List<Group> getGroups() {
		return out(AuthRelationships.HAS_ROLE).toList(Group.class);
	}

	public void addGroup(Group group) {
		addEdge(AuthRelationships.HAS_ROLE, group, Group.class);
	}

}
