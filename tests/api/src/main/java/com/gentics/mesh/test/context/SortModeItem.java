package com.gentics.mesh.test.context;

/**
 * An ordered item of a sort mode.
 */
public enum SortModeItem {
	/**
	 * Now come nulls. Currently can be placed at either first or last position.
	 */
	NULLS(false),
	/**
	 * Now come nulls, independently of the requested sort order. Currently can be placed at either first or last position.
	 */
	NULLS_ORDER_INDEPENDENT(false),
	/**
	 * Now come all the capital letters, and lowercase come afterwards.
	 */
	ALL_CAPITALS_FIRST(true),
	/**
	 * Now come letters by groups `capital,lowercase`.
	 */
	CHAR_CAPITALS_FIRST(true),
	/**
	 * Now come all the lowercase letters, and capitals come afterwards
	 */
	ALL_CAPITALS_LAST(true),
	/**
	 * Now come letters by groups `lowercase,capital`.
	 */
	CHAR_CAPITALS_LAST(true),
	/**
	 * Now come digits. Currently can be placed before or after one of the char based sort mode items.
	 */
	DIGITS(false),
	/**
	 * Sorting test should be skipped.
	 */
	OFF(false);

	private final boolean isCharBased;

	private SortModeItem(boolean isCharBased) {
		this.isCharBased = isCharBased;
	}

	/**
	 * Does this item rely on chars existing in the sorted items around?
	 * 
	 * @return
	 */
	public boolean isCharBased() {
		return isCharBased;
	}
}
