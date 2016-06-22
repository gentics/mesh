package com.gentics.mesh.api.common;

public enum SortOrder {

	ASCENDING("asc"),

	DESCENDING("desc"),

	UNSORTED("unsorted");

	private String simpleName;

	private SortOrder(String simpleName) {
		this.simpleName = simpleName;
	}

	public String getSimpleName() {
		return simpleName;
	}

	/**
	 * Convert the human readable name into a SortOrder object.
	 * 
	 * @param name
	 * @return
	 */
	public static SortOrder valueOfName(String name) {
		if (name == null) {
			return null;
		}
		for (SortOrder p : SortOrder.values()) {
			if (name.equals(p.getSimpleName())) {
				return p;
			}
		}
		return null;
	}

}
