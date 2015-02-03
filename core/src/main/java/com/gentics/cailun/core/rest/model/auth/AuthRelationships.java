package com.gentics.cailun.core.rest.model.auth;

import org.neo4j.graphdb.RelationshipType;

public enum AuthRelationships implements RelationshipType {
	HAS_PERMISSIONSET, MEMBER_OF, HAS_ROLE
}
