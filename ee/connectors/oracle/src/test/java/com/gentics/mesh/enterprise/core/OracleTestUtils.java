package com.gentics.mesh.enterprise.core;

import java.util.Comparator;

/**
 * Oracle specific test utils.
 */
public class OracleTestUtils {
	/**
	 * Sort comparator, which sorts like Oracle does, when using the BINARY collation for sorting
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

	/**
	 * Allowed minimum value for NUMBER data type
	 * @see https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/Data-Types.html#GUID-9401BC04-81C4-4CD5-99E7-C5E25C83F608
	 * @return limit
	 */
	public static Double getMinAllowedDouble() {
		return 1E-130;
	}

	/**
	 * Allowed maximum value for NUMBER data type
	 * @see https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/Data-Types.html#GUID-9401BC04-81C4-4CD5-99E7-C5E25C83F608
	 * @return limit
	 */
	public static Double getMaxAllowedDouble() {
		return 1E125;
	}
}
