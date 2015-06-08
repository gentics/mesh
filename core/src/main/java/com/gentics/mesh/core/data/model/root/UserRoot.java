package com.gentics.mesh.core.data.model.root;

import com.gentics.mesh.core.data.model.generic.AbstractPersistable;
import com.gentics.mesh.core.data.model.relationship.BasicRelationships;
import com.gentics.mesh.core.data.model.tinkerpop.User;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;

public interface UserRoot extends AbstractPersistable {

	@Adjacency(label = BasicRelationships.HAS_USER, direction = Direction.OUT)
	public Iterable<User> getUsers();

	@Adjacency(label = BasicRelationships.HAS_USER, direction = Direction.OUT)
	public void addUser(User user);

	@Adjacency(label = BasicRelationships.HAS_USER, direction = Direction.OUT)
	public void removeUser(User user);

	// TODO unique index

}
