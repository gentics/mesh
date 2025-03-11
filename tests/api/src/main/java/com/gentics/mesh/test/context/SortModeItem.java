package com.gentics.mesh.test.context;

/**
 * An ordered item of a sort mode.
 */
public enum SortModeItem {
	/**
	 * Now come nulls
	 */
	NULLS,
	/**
	 * Now come nulls, independently of the requested sort order
	 */
	NULLS_ORDER_INDEPENDENT,
	/**
	 * Now come all the capital letters, and lowercase come afterwards.
	 */
	ALL_CAPITALS_FIRST,
	/**
	 * Now come letters by groups `capital,lowercase`.
	 */
	CHAR_CAPITALS_FIRST,
	/**
	 * Now come all the lowercase letters, and capitals come afterwards
	 */
	ALL_CAPITALS_LAST,
	/**
	 * Now come letters by groups `lowercase,capital`.
	 */
	CHAR_CAPITALS_LAST,
	/**
	 * Now come digits
	 */
	DIGITS,
}
