package com.gentics.cailun.core.rest.model.relationship;

import org.neo4j.graphdb.RelationshipType;

public class BasicRelationships {

	public static enum TYPES implements RelationshipType {
		LINKED, HAS_SUB_TAG, ASSIGNED_TO_PROJECT, HAS_USER, HAS_PROJECT
	}

	public static final String LINKED = "LINKED";
	public static final String HAS_SUB_TAG = "HAS_SUB_TAG";
	public static final String ASSIGNED_TO_PROJECT = "ASSIGNED_TO_PROJECT";
	public static final String HAS_USER = "HAS_USER";
	public static final String HAS_PROJECT = "HAS_PROJECT";
	public static final String HAS_ROOT_GROUP = "HAS_ROOT_GROUP";
	public static final String HAS_ROOT_TAG = "HAS_ROOT_TAG";
	public static final String HAS_CREATOR = "HAS_CREATOR";
	public static final String PARENT_OF = "PARENT_OF";
	public static final String HAS_LOCALIZED_CONTENT = "HAS_CONTENT";
	public static final String HAS_LOCALIZED_TAGS = "HAS_LOCALIZED_TAGS";
	public static final String HAS_LANGUAGE = "HAS_LANGUAGE";
	public static final String HAS_FILE = "HAS_FILE";
	public static final String HAS_LOCALISATION = "HAS_LOCALISATION";
}
