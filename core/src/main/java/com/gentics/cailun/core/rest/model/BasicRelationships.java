package com.gentics.cailun.core.rest.model;

import org.neo4j.graphdb.RelationshipType;

public class BasicRelationships {
	public static enum TYPES implements RelationshipType {
		LINKED, TAGGED, ASSIGNED_TO_PROJECT, HAS_USER, HAS_PROJECT
	}

	public static final String LINKED = "LINKED";
	public static final String TAGGED = "TAGGED";
	public static final String ASSIGNED_TO_PROJECT = "ASSIGNED_TO_PROJECT";
	public static final String HAS_USER = "HAS_USER";
	public static final String HAS_PROJECT = "HAS_PROJECT";
	public static final String HAS_ROOT_GROUP = "HAS_ROOT_GROUP";
	public static final String HAS_ROOT_TAG = "HAS_ROOT_TAG";
	public static final String HAS_CREATOR = "HAS_CREATOR";
}
