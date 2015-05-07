package com.gentics.mesh.core.data.model.auth;

import org.springframework.data.neo4j.annotation.RelationshipEntity;

import com.gentics.mesh.core.data.model.generic.GenericNode;

@RelationshipEntity
public class CreatePermission extends GraphPermission {

	public CreatePermission() {
		// TODO Auto-generated constructor stub
	}
	
	public CreatePermission(Role role, GenericNode targetNode) {
		super(role, targetNode);
	}

}
