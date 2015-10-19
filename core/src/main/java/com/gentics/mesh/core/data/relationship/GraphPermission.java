package com.gentics.mesh.core.data.relationship;

import com.gentics.mesh.graphdb.spi.Database;

/**
 * Internal enum which provides labels for graph permission edges that are created between the target element and a role.
 */
public enum GraphPermission {
	CREATE_PERM("HAS_CREATE_PERMISSION", "create"), READ_PERM("HAS_READ_PERMISSION", "read"), UPDATE_PERM("HAS_UPDATE_PERMISSION",
			"update"), DELETE_PERM("HAS_DELETE_PERMISSION", "delete");

	public static void checkIndices(Database database) {
		for (String label : GraphPermission.labels()) {
			database.addEdgeIndex(label);
		}
	}

	private String label;
	private String simpleName;

	GraphPermission(String label, String simpleName) {
		this.label = label;
		this.simpleName = simpleName;
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
	 * Convert the human readable permission name back into a graph permission object.
	 * 
	 * @param simpleName
	 * @return
	 */
	public static GraphPermission valueOfSimpleName(String simpleName) {
		for (GraphPermission p : GraphPermission.values()) {
			if (simpleName.equals(p.getSimpleName())) {
				return p;
			}
		}
		return null;
	}

	/**
	 * Return the human friendly name for the permission.
	 * 
	 * @return
	 */
	public String getSimpleName() {
		return simpleName;
	}

	@Override
	public String toString() {
		return label;
	}
}
