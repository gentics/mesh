package com.gentics.mesh.core.rest;

import org.apache.commons.lang.StringUtils;

/**
 * Enum which contains the sortorder states.
 */
public enum SortOrder {

	ASCENDING("asc"),

	DESCENDING("desc"),

	UNSORTED(StringUtils.EMPTY);

	private String value;

	private SortOrder(String value) {
		this.value = value;
	}

	/**
	 * Return the value of the sortorder.
	 * 
	 * @return
	 */
	public String getValue() {
		return value;
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
			if (name.equals(p.getValue())) {
				return p;
			}
		}
		return null;
	}

}
