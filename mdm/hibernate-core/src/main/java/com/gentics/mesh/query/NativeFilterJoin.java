package com.gentics.mesh.query;

/**
 * A join interface to use with SQL Native filtering.
 * 
 * @author plyhun
 *
 */
public interface NativeFilterJoin {

	/**
	 * Make a FROM section JOIN line from this instance
	 * 
	 * @return
	 */
	String toSqlJoinString();
}
