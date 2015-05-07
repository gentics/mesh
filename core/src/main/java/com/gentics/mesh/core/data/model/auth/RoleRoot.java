package com.gentics.mesh.core.data.model.auth;

import java.util.HashSet;
import java.util.Set;

import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

import com.gentics.mesh.core.data.model.generic.AbstractPersistable;
import com.gentics.mesh.core.data.model.relationship.BasicRelationships;

@NodeEntity
public class RoleRoot extends AbstractPersistable {

	private static final long serialVersionUID = 3295656607153012001L;

	@RelatedTo(type = BasicRelationships.HAS_ROLE, direction = Direction.OUTGOING, elementClass = Role.class)
	private Set<Role> roles = new HashSet<>();
	
	@Indexed(unique = true)
	private String unique = RoleRoot.class.getSimpleName();

	public RoleRoot() {
	}
	
	public Set<Role> getRoles() {
		return roles;
	}

}
