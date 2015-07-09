package com.gentics.mesh.core.data.relationship;

public enum Permission {
	CREATE_PERM("HAS_CREATE_PERMISSION", "create"), READ_PERM("HAS_READ_PERMISSION", "read"), UPDATE_PERM("HAS_UPDATE_PERMISSION", "update"), DELETE_PERM(
			"HAS_DELETE_PERMISSION", "delete");

	private String label;
	private String humanName;

	Permission(String label, String humanName) {
		this.label = label;
		this.humanName = humanName;
	}

	public static String[] labels() {
		Permission[] permissions = values();
		String[] names = new String[permissions.length];

		for (int i = 0; i < permissions.length; i++) {
			names[i] = permissions[i].label();
		}

		return names;
	}

	public String label() {
		return label;
	}

	public static Permission valueOfLabel(String labelName) {
		for (Permission p : Permission.values()) {
			if (labelName.equals(p.label())) {
				return p;
			}
		}
		return null;
	}

	public String getHumanName() {
		return humanName;
	}

	@Override
	public String toString() {
		return label;
	}
}
