package com.gentics.mesh.enterprise.core;

import java.util.Comparator;

/**
 * Postgres Specific Test Utilities
 */
public final class PostgresTestUtils {

	private final static Comparator<Character> CHAR_COMPARATOR = (a, b) -> {
		if (a == b) {
			return 0;
		}
		if (a == null) {
			return -1;
		}
		if (b == null) {
			return 1;
		}
		if (!Character.isDigit(a) && Character.isDigit(b)) {
			return 1;
		}
		if (Character.isDigit(a) && !Character.isDigit(b)) {
			return -1;
		}
		if ('_' == a) {
			a = ' ';
		}
		if ('_' == b) {
			b = ' ';
		}
		return Character.valueOf(Character.toLowerCase(a)).compareTo(Character.toLowerCase(b));
	};

	/**
	 * Sort comparator, which sorts like Postgres does
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
		int lenA = a.length();
		int lenB = b.length();
		for (int i = 0; i < Math.max(lenA, lenB); i++) {
			Character ca = i < lenA ? a.charAt(i) : null;
			Character cb = i < lenB ? b.charAt(i) : null;
			int res = CHAR_COMPARATOR.compare(ca, cb);
			if (res != 0) {
				return res;
			}
		}
		return 0;
	};
}
