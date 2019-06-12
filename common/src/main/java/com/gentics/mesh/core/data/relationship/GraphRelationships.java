package com.gentics.mesh.core.data.relationship;

import static com.gentics.mesh.madl.type.EdgeTypeDefinition.edgeType;

import com.gentics.mesh.graphdb.spi.IndexHandler;
import com.gentics.mesh.graphdb.spi.TypeHandler;

/**
 * Main class that holds all the graph relationship names.
 */
public class GraphRelationships {

	/**
	 * Initialise the graph database by adding all needed edge types and indices.
	 * 
	 * @param db
	 */
	public static void init(TypeHandler type, IndexHandler index) {

		type.createType(edgeType(HAS_INITIAL_BRANCH));
		type.createType(edgeType(HAS_LATEST_BRANCH));
		type.createType(edgeType(HAS_NEXT_BRANCH));
		type.createType(edgeType(HAS_NODE));
		type.createType(edgeType(HAS_NODE_REFERENCE));
		type.createType(edgeType(ASSIGNED_TO_PROJECT));
		type.createType(edgeType(HAS_GROUP_ROOT));
		type.createType(edgeType(HAS_USER));
		type.createType(edgeType(HAS_ROLE));
		type.createType(edgeType(ASSIGNED_TO_ROLE));

		type.createType(edgeType(HAS_TAG_ROOT));
		type.createType(edgeType(HAS_TAG_FAMILY));

		type.createType(edgeType(HAS_CREATOR));
		type.createType(edgeType(HAS_EDITOR));
		type.createType(edgeType(HAS_LANGUAGE));
		type.createType(edgeType(HAS_PARENT_NODE));
		type.createType(edgeType(HAS_ROOT_NODE));
		type.createType(edgeType(HAS_JOB_ROOT));

		type.createType(edgeType(HAS_SCHEMA_TYPE));

		type.createType(edgeType(HAS_SCHEMA_CONTAINER));
		type.createType(edgeType(HAS_MICROSCHEMA_CONTAINER));
		type.createType(edgeType(HAS_MICROSCHEMA_ROOT));

		type.createType(edgeType(HAS_NODE_ROOT));
		type.createType(edgeType(HAS_TAGFAMILY_ROOT));
		type.createType(edgeType(HAS_ROLE_ROOT));
		type.createType(edgeType(HAS_USER_ROOT));
		type.createType(edgeType(HAS_LANGUAGE_ROOT));
		type.createType(edgeType(HAS_PROJECT_ROOT));
		type.createType(edgeType(HAS_BRANCH_ROOT));

		// type.createType(edgeType(HAS_FIELD);
		type.createType(edgeType(HAS_ITEM));
		type.createType(edgeType(HAS_LIST));

		// Versioning
		type.createType(edgeType(HAS_CHANGE));
		type.createType(edgeType(HAS_VERSION));
		type.createType(edgeType(HAS_LATEST_VERSION));
		type.createType(edgeType(HAS_PARENT_CONTAINER));
		type.createType(edgeType(HAS_SCHEMA_CONTAINER_VERSION));

		// Branches
		type.createType(edgeType(HAS_SCHEMA_VERSION));
		type.createType(edgeType(HAS_MICROSCHEMA_VERSION));
		type.createType(edgeType(HAS_BRANCH_TAG));

		// Jobs
		type.createType(edgeType(HAS_JOB));
		type.createType(edgeType(HAS_FROM_VERSION));
		type.createType(edgeType(HAS_TO_VERSION));

		// Changelog
		type.createType(edgeType(HAS_CHANGELOG_ROOT));

		// Binary
		type.createType(edgeType(HAS_BINARY_ROOT));
		type.createType(edgeType(HAS_BINARY));

	}

	// Project
	public static final String HAS_PROJECT = "HAS_PROJECT";

	// Branch
	public static final String HAS_BRANCH = "HAS_BRANCH";
	public static final String HAS_INITIAL_BRANCH = "HAS_INITIAL_BRANCH";
	public static final String HAS_LATEST_BRANCH = "HAS_LATEST_BRANCH";
	public static final String HAS_NEXT_BRANCH = "HAS_NEXT_BRANCH";

	// Nodes
	public static final String HAS_NODE = "HAS_NODE";
	public static final String HAS_NODE_REFERENCE = "HAS_NODE_REFERENCE";
	public static final String ASSIGNED_TO_PROJECT = "ASSIGNED_TO_PROJECT";

	// Groups/User/Role
	public static final String HAS_GROUP_ROOT = "HAS_GROUP_ROOT";
	public static final String HAS_USER = "HAS_USER";
	public static final String HAS_GROUP = "HAS_GROUP";
	public static final String HAS_ROLE = "HAS_ROLE";
	public static final String ASSIGNED_TO_ROLE = "ASSIGNED_TO_ROLE";

	// Tags
	public static final String HAS_TAG_ROOT = "HAS_TAG_ROOT";
	public static final String HAS_TAG_FAMILY = "HAS_TAG_FAMILY";
	public static final String HAS_TAG = "HAS_TAG";

	// Misc
	public static final String HAS_CREATOR = "HAS_CREATOR";
	public static final String HAS_EDITOR = "HAS_EDITOR";
	public static final String HAS_LANGUAGE = "HAS_LANGUAGE";
	public static final String HAS_FIELD_CONTAINER = "HAS_FIELD_CONTAINER";

	public static final String HAS_PARENT_NODE = "HAS_PARENT_NODE";

	// Jobs
	public static final String HAS_JOB = "HAS_JOB";
	public static final String HAS_TO_VERSION = "HAS_TO_VERSION";
	public static final String HAS_FROM_VERSION = "HAS_FROM_VERSION";

	/**
	 * Edge type which is used to connect project and base node
	 */
	public static final String HAS_ROOT_NODE = "HAS_ROOT_NODE";

	// Schemas
	public static final String HAS_SCHEMA_TYPE = "HAS_SCHEMA_TYPE";
	public static final String HAS_SCHEMA_ROOT = "HAS_ROOT_SCHEMA";
	public static final String HAS_SCHEMA_CONTAINER = "HAS_SCHEMA_CONTAINER";
	public static final String HAS_SCHEMA_CONTAINER_VERSION = "HAS_SCHEMA_CONTAINER_VERSION";
	public static final String HAS_PARENT_CONTAINER = "HAS_PARENT_CONTAINER";
	public static final String HAS_SCHEMA_CONTAINER_ITEM = "HAS_SCHEMA_CONTAINER_ITEM";
	public static final String HAS_MICROSCHEMA_CONTAINER = "HAS_MICROSCHEMA_CONTAINER";

	// Roots
	public static final String HAS_NODE_ROOT = "HAS_NODE_ROOT";
	public static final String HAS_TAGFAMILY_ROOT = "HAS_TAGFAMILY_ROOT";
	public static final String HAS_MICROSCHEMA_ROOT = "HAS_MICROSCHEMA_ROOT";
	public static final String HAS_ROLE_ROOT = "HAS_ROLE_ROOT";
	public static final String HAS_USER_ROOT = "HAS_USER_ROOT";
	public static final String HAS_LANGUAGE_ROOT = "HAS_LANGUAGE_ROOT";
	public static final String HAS_PROJECT_ROOT = "HAS_PROJECT_ROOT";
	public static final String HAS_BRANCH_ROOT = "HAS_BRANCH_ROOT";
	public static final String HAS_JOB_ROOT = "HAS_JOB_ROOT";

	// Asset root
	public static final String HAS_BINARY_ROOT = "HAS_BINARY_ROOT";
	public static final String HAS_BINARY = "HAS_BINARY";

	public static final String HAS_FIELD = "HAS_FIELD";
	public static final String HAS_ITEM = "HAS_ITEM";

	public static final String HAS_LIST = "HAS_LIST";

	// Versioning
	public static final String HAS_VERSION = "HAS_VERSION";
	public static final String HAS_CHANGE = "HAS_CHANGE";
	public static final String HAS_LATEST_VERSION = "HAS_LATEST_VERSION";

	// Branches
	public static final String HAS_SCHEMA_VERSION = "HAS_SCHEMA_VERSION";
	public static final String HAS_MICROSCHEMA_VERSION = "HAS_MICROSCHEMA_VERSION";
	public static final String HAS_BRANCH_TAG = "HAS_BRANCH_TAG";

	// Changelog system
	public static final String HAS_CHANGELOG_ROOT = "HAS_CHANGELOG_ROOT";

}
