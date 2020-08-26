package com.gentics.mesh.core.data.perm;

import static com.gentics.mesh.core.rest.common.Permission.CREATE;
import static com.gentics.mesh.core.rest.common.Permission.DELETE;
import static com.gentics.mesh.core.rest.common.Permission.PUBLISH;
import static com.gentics.mesh.core.rest.common.Permission.READ;
import static com.gentics.mesh.core.rest.common.Permission.READ_PUBLISHED;
import static com.gentics.mesh.core.rest.common.Permission.UPDATE;

import com.gentics.mesh.core.rest.common.Permission;

/**
 * Internal enum which provides labels for graph permission edges that are created between the target element and a role.
 */
public enum InternalPermission {

	CREATE_PERM("permission_create", CREATE),

	READ_PERM("permission_read", READ),

	UPDATE_PERM("permission_update", UPDATE),

	DELETE_PERM("permission_delete", DELETE),

	READ_PUBLISHED_PERM("permission_read_published", READ_PUBLISHED),

	PUBLISH_PERM("permission_publish", PUBLISH);

	private String propertyKey;
	private Permission restPerm;

	/**
	 * Create a new graph permission.
	 * 
	 * @param propertyKey
	 * @param restPerm
	 *            Rest permission representation
	 */
	InternalPermission(String propertyKey, Permission restPerm) {
		this.propertyKey = propertyKey;
		this.restPerm = restPerm;
	}

	/**
	 * Return an array of graph permission labels.
	 * 
	 * @return
	 */
	public static String[] labels() {
		InternalPermission[] permissions = values();
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
	public static InternalPermission valueOfLabel(String propertyKey) {
		for (InternalPermission p : InternalPermission.values()) {
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

	public static InternalPermission[] basicPermissions() {
		return new InternalPermission[] { CREATE_PERM, READ_PERM, UPDATE_PERM, DELETE_PERM };
	}
}
