package com.gentics.cailun.core.rest.model.auth;

import org.neo4j.graphdb.RelationshipType;

public final class AuthRelationships {
	public static enum TYPES implements RelationshipType {
		HAS_PERMISSION, MEMBER_OF, HAS_ROLE, ASSIGNED_TO
	}

	public static final String HAS_PERMISSION = "HAS_PERMISSION";
	public static final String MEMBER_OF = "MEMBER_OF";
	public static final String HAS_ROLE = "HAS_ROLE";
	public static final String ASSIGNED_TO = "ASSIGNED_TO";
}
