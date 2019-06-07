package com.gentics.mesh.core.data.relationship;

import static com.gentics.mesh.core.rest.common.Permission.CREATE;
import static com.gentics.mesh.core.rest.common.Permission.DELETE;
import static com.gentics.mesh.core.rest.common.Permission.PUBLISH;
import static com.gentics.mesh.core.rest.common.Permission.READ;
import static com.gentics.mesh.core.rest.common.Permission.READ_PUBLISHED;
import static com.gentics.mesh.core.rest.common.Permission.UPDATE;
import static com.syncleus.ferma.index.EdgeIndexDefinition.edgeIndex;
import static com.syncleus.ferma.type.EdgeTypeDefinition.edgeType;

import com.gentics.mesh.core.rest.common.Permission;
import com.gentics.mesh.graphdb.spi.Database;

/**
 * Internal enum which provides labels for graph permission edges that are created between the target element and a role.
 */
public enum GraphPermission {

	CREATE_PERM("HAS_CREATE_PERMISSION", CREATE),

	READ_PERM("HAS_READ_PERMISSION", READ),

	UPDATE_PERM("HAS_UPDATE_PERMISSION", UPDATE),

	DELETE_PERM("HAS_DELETE_PERMISSION", DELETE),

	READ_PUBLISHED_PERM("HAS_READ_PUBLISHED_PERMISSION", READ_PUBLISHED),

	PUBLISH_PERM("HAS_PUBLISH_PERMISSION", PUBLISH);

	public static void init(Database database) {
		for (String label : GraphPermission.labels()) {
			database.createType(edgeType(label));
			database.createIndex(edgeIndex(label).withInOut());
		}
	}

	private String label;
	private Permission restPerm;

	/**
	 * Create a new graph permission.
	 * 
	 * @param label
	 * @param restPerm
	 *            Rest permission representation
	 */
	GraphPermission(String label, Permission restPerm) {
		this.label = label;
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
			names[i] = permissions[i].label();
		}

		return names;
	}

	/**
	 * Return the label of the graph permission.
	 * 
	 * @return
	 */
	public String label() {
		return label;
	}

	/**
	 * Convert a label name back into a graph permission object.
	 * 
	 * @param labelName
	 * @return
	 */
	public static GraphPermission valueOfLabel(String labelName) {
		for (GraphPermission p : GraphPermission.values()) {
			if (labelName.equals(p.label())) {
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
		return label;
	}

	public static GraphPermission[] basicPermissions() {
		return new GraphPermission[] { CREATE_PERM, READ_PERM, UPDATE_PERM, DELETE_PERM };
	}
}
