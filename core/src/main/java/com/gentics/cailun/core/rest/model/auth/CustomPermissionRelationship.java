package com.gentics.cailun.core.rest.model.auth;

import lombok.NoArgsConstructor;

import com.gentics.cailun.core.rest.model.GenericNode;

@NoArgsConstructor
public class CustomPermissionRelationship extends BasicPermissionRelationship {
	private String blub = "huhu";

	public CustomPermissionRelationship(Role adminRole, GenericNode currentNode) {
		super(adminRole, currentNode);
	}

}
