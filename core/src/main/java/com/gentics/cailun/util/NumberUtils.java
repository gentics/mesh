package com.gentics.cailun.util;

/**
 * Extension of the basic number utils
 * @author johannes2
 *
 */
public class NumberUtils extends org.apache.commons.lang3.math.NumberUtils {

	/**
	 * <p>
	 * Returns a the long value of the default value when the long value is null
	 * </p>
	 *
	 * <p>
	 * If the value is <code>null</code>, the default value is returned.
	 * </p>
	 *
	 * <pre>
	 *   NumberUtils.toLong(null, 1L) = 1L
	 *   NumberUtils.toLong(2L, 1L)   = 2L
	 *   NumberUtils.toLong(new Long(-1L), 0L)  = -1L
	 * </pre>
	 *
	 * @param value
	 *            the value that can be used, may be null
	 * @param defaultValue
	 *            the default value
	 * @return the long represented by the value, or the default if conversion fails
	 */
	public static long toLong(Long value, long defaultValue) {
		if (value == null) {
			return defaultValue;
		} else {
			return value.intValue();
		}
	}

}
