package com.gentics.mesh.core.data.relationship;

import static com.gentics.mesh.core.rest.common.Permission.CREATE;
import static com.gentics.mesh.core.rest.common.Permission.DELETE;
import static com.gentics.mesh.core.rest.common.Permission.PUBLISH;
import static com.gentics.mesh.core.rest.common.Permission.READ;
import static com.gentics.mesh.core.rest.common.Permission.READ_PUBLISHED;
import static com.gentics.mesh.core.rest.common.Permission.UPDATE;
import static com.gentics.mesh.madl.index.EdgeIndexDefinition.edgeIndex;
import static com.gentics.mesh.madl.type.EdgeTypeDefinition.edgeType;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.core.rest.common.Permission;

/**
 * Internal enum which provides labels for graph permission edges that are created between the target element and a role.
 */
public enum GraphPermission {

	CREATE_PERM("permission_create", CREATE),

	READ_PERM("permission_read", READ),

	UPDATE_PERM("permission_update", UPDATE),

	DELETE_PERM("permission_delete", DELETE),

	READ_PUBLISHED_PERM("permission_read_published", READ_PUBLISHED),

	PUBLISH_PERM("permission_publish", PUBLISH);

	public static void init(TypeHandler type, IndexHandler index) {
		for (String label : GraphPermission.labels()) {
			type.createType(edgeType(label));
			index.createIndex(edgeIndex(label).withInOut());
		}
	}

	private String propertyKey;
	private Permission restPerm;

	/**
	 * Create a new graph permission.
	 * 
	 * @param propertyKey
	 * @param restPerm
	 *            Rest permission representation
	 */
	GraphPermission(String propertyKey, Permission restPerm) {
		this.propertyKey = propertyKey;
		this.restPerm = restPerm;
	}

	/**
	 * Return an array of graph permission labels.
	 * 
	 * @return
	 */
	public static String[] labels() {
		GraphPermission[] permissions = values();
		String[] names = new String[permissions.length];

		for (int i = 0; i < permissions.length; i++) {
			names[i] = permissions[i].propertyKey();
		}

		return names;
	}

	/**
	 * Return the label of the graph permission.
	 * 
	 * @return
	 */
	public String propertyKey() {
		return propertyKey;
	}

	/**
	 * Convert a key name back into a graph permission object.
	 * 
	 * @param propertyKey
	 * @return
	 */
	public static GraphPermission valueOfLabel(String propertyKey) {
		for (GraphPermission p : GraphPermission.values()) {
			if (propertyKey.equals(p.propertyKey())) {
				return p;
			}
		}
		return null;
	}

	/**
	 * Return the rest permission representation.
	 * 
	 * @return
	 */
	public Permission getRestPerm() {
		return restPerm;
	}

	@Override
	public String toString() {
		return propertyKey;
	}

	public static GraphPermission[] basicPermissions() {
		return new GraphPermission[] { CREATE_PERM, READ_PERM, UPDATE_PERM, DELETE_PERM };
	}
}
