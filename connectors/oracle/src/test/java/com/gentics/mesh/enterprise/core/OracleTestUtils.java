package com.gentics.mesh.enterprise.core;

import java.util.Comparator;

/**
 * Oracle specific test utils.
 */
public class OracleTestUtils {
	/**
	 * Sort comparator, which sorts like Oracle does
	 */
	public final static Comparator<String> SORT_COMPARATOR = (a,b) -> {
		if (a == b) {
			return 0;
		}
		if (a == null) {
			return 1;
		}
		if (b == null) {
			return -1;
		}
		if (!Character.isDigit(a.charAt(0)) && Character.isDigit(b.charAt(0))) {
			return 1;
		}
		if (Character.isDigit(a.charAt(0)) && !Character.isDigit(b.charAt(0))) {
			return -1;
		}
		return a.compareTo(b);
	};
}
