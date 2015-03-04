package com.gentics.cailun.core.data.model.auth;

import java.util.HashSet;
import java.util.Set;

import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.Fetch;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedToVia;

import com.gentics.cailun.core.data.model.generic.GenericNode;

@NodeEntity
public class Role extends GenericNode {

	private static final long serialVersionUID = -6696156556292877992L;

	private String name;

	@Fetch
	@RelatedToVia(type = AuthRelationships.HAS_PERMISSION, direction = Direction.OUTGOING, elementClass = GraphPermission.class)
	protected Set<GraphPermission> permissions = new HashSet<>();

	public boolean addPermission(GraphPermission permission) {
		return permissions.add(permission);
	}

	@SuppressWarnings("unused")
	private Role() {
	}

	public Role(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

}
