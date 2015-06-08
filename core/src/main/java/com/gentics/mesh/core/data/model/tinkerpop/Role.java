package com.gentics.mesh.core.data.model.tinkerpop;

import com.gentics.mesh.core.data.model.auth.AuthRelationships;
import com.gentics.mesh.core.data.model.generic.GenericNode;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;

public interface Role extends GenericNode {

	//TODO index on name
	@Property("name")
	public String getName();

	@Property("name")
	public void setName(String name);

	@Adjacency(label = AuthRelationships.HAS_PERMISSION, direction = Direction.OUT)
	public Iterable<GraphPermission> getPermissions();

	@Adjacency(label = AuthRelationships.HAS_PERMISSION, direction = Direction.OUT)
	public void addPermission(GraphPermission permission);

	@Adjacency(label = AuthRelationships.HAS_ROLE, direction = Direction.OUT)
	public Iterable<Group> getGroups();

	@Adjacency(label = AuthRelationships.HAS_ROLE, direction = Direction.OUT)
	public void addGroup(Group group);

}
