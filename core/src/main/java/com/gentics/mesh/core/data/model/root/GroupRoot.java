package com.gentics.mesh.core.data.model.root;

import com.gentics.mesh.core.data.model.generic.AbstractPersistable;
import com.gentics.mesh.core.data.model.relationship.BasicRelationships;
import com.gentics.mesh.core.data.model.tinkerpop.Group;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;

public interface GroupRoot extends AbstractPersistable {

	//TODO unique node
	
	@Adjacency(label = BasicRelationships.HAS_GROUP, direction = Direction.OUT)
	public Iterable<Group> getGroups();

	@Adjacency(label = BasicRelationships.HAS_GROUP, direction = Direction.OUT)
	public void addGroup(Group group);

}
