package com.gentics.mesh.test;

import java.util.Comparator;

/**
 * Default sorting comparators, used in the sorting tests.
 */
public final class MeshSortComparators {

	private MeshSortComparators() {}

	/**
	 * The default comparator.
	 */
	public static final Comparator<String> DEFAULT_COMPARATOR = (a,b) -> {
		if (a == b) {
			return 0;
		}
		if (a == null) {
			return -1;
		}
		if (b == null) {
			return 1;
		}
		if (!Character.isDigit(a.charAt(0)) && Character.isDigit(b.charAt(0))) {
			return 1;
		}
		if (Character.isDigit(a.charAt(0)) && !Character.isDigit(b.charAt(0))) {
			return -1;
		}
		return a.compareTo(b);
	};

	/**
	 * Comparator that accepts any order.
	 */
	public static final Comparator<String> NO_COMPARATOR = (a,b) -> {
		return 0;
	};
}
