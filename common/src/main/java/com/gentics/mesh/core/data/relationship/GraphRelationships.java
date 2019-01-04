package com.gentics.mesh.core.data.relationship;

import com.gentics.mesh.graphdb.spi.LegacyDatabase;

/**
 * Main class that holds all the graph relationship names.
 */
public class GraphRelationships {

	/**
	 * Initialise the graph database by adding all needed edge types and indices.
	 * 
	 * @param db
	 */
	public static void init(LegacyDatabase db) {

		db.addEdgeType(HAS_INITIAL_BRANCH);
		db.addEdgeType(HAS_LATEST_BRANCH);
		db.addEdgeType(HAS_NEXT_BRANCH);
		db.addEdgeType(HAS_NODE);
		db.addEdgeType(HAS_NODE_REFERENCE);
		db.addEdgeType(ASSIGNED_TO_PROJECT);
		db.addEdgeType(HAS_GROUP_ROOT);
		db.addEdgeType(HAS_USER);
		db.addEdgeType(HAS_ROLE);

		db.addEdgeType(HAS_TAG_ROOT);
		db.addEdgeType(HAS_TAG_FAMILY);

		db.addEdgeType(HAS_CREATOR);
		db.addEdgeType(HAS_EDITOR);
		db.addEdgeType(HAS_LANGUAGE);
		db.addEdgeType(HAS_PARENT_NODE);
		db.addEdgeType(HAS_ROOT_NODE);
		db.addEdgeType(HAS_JOB_ROOT);

		db.addEdgeType(HAS_SCHEMA_TYPE);

		db.addEdgeType(HAS_SCHEMA_CONTAINER);
		db.addEdgeType(HAS_MICROSCHEMA_CONTAINER);
		db.addEdgeType(HAS_MICROSCHEMA_ROOT);

		db.addEdgeType(HAS_NODE_ROOT);
		db.addEdgeType(HAS_TAGFAMILY_ROOT);
		db.addEdgeType(HAS_ROLE_ROOT);
		db.addEdgeType(HAS_USER_ROOT);
		db.addEdgeType(HAS_LANGUAGE_ROOT);
		db.addEdgeType(HAS_PROJECT_ROOT);
		db.addEdgeType(HAS_BRANCH_ROOT);

		// db.addEdgeType(HAS_FIELD);
		db.addEdgeType(HAS_ITEM);
		db.addEdgeType(HAS_LIST);

		// Versioning
		db.addEdgeType(HAS_CHANGE);
		db.addEdgeType(HAS_VERSION);
		db.addEdgeType(HAS_LATEST_VERSION);
		db.addEdgeType(HAS_PARENT_CONTAINER);
		db.addEdgeType(HAS_SCHEMA_CONTAINER_VERSION);

		// Branches
		db.addEdgeType(HAS_SCHEMA_VERSION);
		db.addEdgeType(HAS_MICROSCHEMA_VERSION);
		db.addEdgeType(HAS_BRANCH_TAG);

		// Jobs
		db.addEdgeType(HAS_JOB);
		db.addEdgeType(HAS_FROM_VERSION);
		db.addEdgeType(HAS_TO_VERSION);

		// Changelog
		db.addEdgeType(HAS_CHANGELOG_ROOT);

		// Binary
		db.addEdgeType(HAS_BINARY_ROOT);
		db.addEdgeType(HAS_BINARY);

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
