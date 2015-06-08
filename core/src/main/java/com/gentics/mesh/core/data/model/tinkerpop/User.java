package com.gentics.mesh.core.data.model.tinkerpop;

import com.gentics.mesh.core.data.model.auth.AuthRelationships;
import com.gentics.mesh.core.data.model.generic.GenericNode;
import com.gentics.mesh.core.data.model.relationship.BasicRelationships;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.annotations.gremlin.GremlinGroovy;

public interface User extends GenericNode {

	@Property("firstname")
	public String getFirstname();

	@Property("firstname")
	public void setFirstname(String name);

	@Property("lastname")
	public String getLastname();

	@Property("lastname")
	public void setLastname(String name);

	//TODO add unique index
	@Property("username")
	public String getUsername();

	@Property("username")
	public void setUsername(String name);

	@Property("emailAddress")
	public String getEmailAddress();

	@Property("emailAddress")
	public void setEmailAddress(String emailAddress);

	@Adjacency(label = AuthRelationships.HAS_USER, direction = Direction.OUT)
	public Iterable<Group> getGroups();

	@Adjacency(label = AuthRelationships.HAS_USER, direction = Direction.OUT)
	public void addGroup(Group group);

	@Adjacency(label = BasicRelationships.HAS_USER)
	@GremlinGroovy(value = "it.out('HAS_USER').count()", frame = false)
	public Long getGroupCount();

	//TODO customize and add java handler?
	@Property("passwordHash")
	public void setPasswordHash(String hash);

	@Property("passwordHash")
	public String getPasswordHash();
	
//	public String getPrincipalId() {
//		return username + "%" + emailAddress + "%" + passwordHash + "#" + getId();
//	}

}
