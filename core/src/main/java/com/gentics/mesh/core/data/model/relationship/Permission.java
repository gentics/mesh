package com.gentics.mesh.core.data.model.relationship;

public enum Permission {
	CREATE_PERM("HAS_CREATE_PERMISSION"), READ_PERM("HAS_READ_PERMISSION"), UPDATE_PERM("HAS_UPDATE_PERMISSION"), DELETE_PERM("HAS_DELETE_PERMISSION");

	private String label;

	Permission(String label) {
		this.label = label;
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

	@Override
	public String toString() {
		return label;
	}
}
