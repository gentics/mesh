package com.gentics.mesh.core.data.relationship;

import static com.gentics.mesh.madl.type.EdgeTypeDefinition.edgeType;

import java.util.HashMap;
import java.util.Map;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.core.data.BranchParentEntry;
import com.gentics.mesh.core.data.MeshVertex;

/**
 * Main class that holds all the graph relationship names.
 */
public class GraphRelationships {

	/**
	 * Add a relation between entities through an edge.
	 */
	public static <K extends MeshVertex, V extends MeshVertex> void addRelation(Class<K> keyClass, Class<V> valueClass, String mappingName, String relationName, String edgeFieldName, String defaultEdgeFieldFilterValue) {
		Map<String, GraphRelationship> relations = VERTEX_RELATIONS.getOrDefault(keyClass, new HashMap<>());
		relations.put(mappingName, new GraphRelationship(relationName, valueClass, edgeFieldName, defaultEdgeFieldFilterValue));
		VERTEX_RELATIONS.put(keyClass, relations);
	}

	/**
	 * Add a relation between entities through UUID.
	 * 
	 * @param <K>
	 * @param <V>
	 * @param keyClass
	 * @param valueClass
	 * @param mappingName
	 * @param relationName
	 */
	public static <K extends MeshVertex, V extends MeshVertex> void addRelation(Class<K> keyClass, Class<V> valueClass, String mappingName) {
		addRelation(keyClass, valueClass, mappingName, MeshVertex.UUID_KEY, mappingName, null);
	}

	/**
	 * Find all relations of a given type.
	 * 
	 * @param keyClass
	 * @return
	 */
	public static Map<String, GraphRelationship> findRelation(Class<?> keyClass) {
		return VERTEX_RELATIONS.get(keyClass);
	}

	/**
	 * Initialise the graph database by adding all needed edge types and indices.
	 * 
	 * @param type
	 * @param index
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

		type.createType(edgeType(HAS_LANGUAGE));
		type.createType(edgeType(HAS_ROOT_NODE));
		type.createType(edgeType(HAS_JOB_ROOT));

		type.createType(edgeType(HAS_SCHEMA_TYPE));

		type.createType(edgeType(HAS_SCHEMA_CONTAINER));
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

		// Image variants
		type.createType(edgeType(HAS_VARIANTS));
		type.createType(edgeType(HAS_FIELD_VARIANTS));
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
	public static final String PROJECT_KEY_PROPERTY = "project";

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
	public static final String CREATOR_UUID_PROPERTY_KEY = "creator";
	public static final String EDITOR_UUID_PROPERTY_KEY = "editor";
	public static final String HAS_LANGUAGE = "HAS_LANGUAGE";
	public static final String HAS_FIELD_CONTAINER = "HAS_FIELD_CONTAINER";

	/**
	 * The value of this property is a set of strings containing all parent node uuids of every branch.
	 */
	public static final String PARENTS_KEY_PROPERTY = "parents";

	/**
	 * The value of this property is a set of strings containing all parent nodes of every branch.
	 * The format of the entries is different from <code>PARENTS_KEY_PROPERTY</code>. Every entry is a combination
	 * of the branch uuid and the parent uuid concatenated with a <code>":"</code>. The {@link BranchParentEntry} class
	 * helps with transforming from/to this format.
	 */
	public static final String BRANCH_PARENTS_KEY_PROPERTY = "branchParents";

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
	public static final String SCHEMA_CONTAINER_KEY_PROPERTY = "schema";
	public static final String SCHEMA_CONTAINER_VERSION_KEY_PROPERTY = "schemaVersion";
	public static final String HAS_PARENT_CONTAINER = "HAS_PARENT_CONTAINER";
	public static final String HAS_SCHEMA_CONTAINER_ITEM = "HAS_SCHEMA_CONTAINER_ITEM";
	public static final String MICROSCHEMA_VERSION_KEY_PROPERTY = "microschema";

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

	public static final String HAS_FIELD = "HAS_FIELD";
	public static final String HAS_ITEM = "HAS_ITEM";
	public static final String HAS_LIST = "HAS_LIST";
	public static final String HAS_VARIANTS = "HAS_VARIANTS";
	public static final String HAS_FIELD_VARIANTS = "HAS_FIELD_VARIANTS";

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

	private static final Map<Class<? extends MeshVertex>, Map<String, GraphRelationship>> VERTEX_RELATIONS = new HashMap<>();
}
