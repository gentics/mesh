package com.gentics.cailun.core.data.model.relationship;

import org.neo4j.graphdb.RelationshipType;

public class BasicRelationships {

	public static enum TYPES implements RelationshipType {
		LINKED, HAS_SUB_TAG, ASSIGNED_TO_PROJECT, HAS_USER, HAS_PROJECT, HAS_I18N_PROPERTIES, HAS_CONTENT
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
	public static final String HAS_LOCALIZED_TAGS = "HAS_LOCALIZED_TAGS";
	public static final String HAS_LANGUAGE = "HAS_LANGUAGE";
	public static final String HAS_LOCALISATION = "HAS_LOCALISATION";
	public static final String HAS_I18N_PROPERTIES = "HAS_I18N_PROPERTIES";
	public static final String HAS_CHILD = "HAS_CHILD";
	public static final String IS_LOCKED = "IS_LOCKED";
	public static final String HAS_PROPERTY_TYPE_SCHEMA = "HAS_PROPERTY_TYPE_SCHEMA";
	public static final String HAS_OBJECT_SCHEMA = "HAS_OBJECT_SCHEMA";
	public static final String HAS_SUB_GROUP = "HAS_SUB_GROUP";
	public static final String HAS_CONTENT = "HAS_CONTENT";
	public static final String HAS_GROUP = "HAS_GROUP";
	public static final String HAS_ROLE = "HAS_ROLE";
	public static final String HAS_SCHEMA = "HAS_SCHEMA";
}
