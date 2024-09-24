package com.gentics.mesh.query;

import com.gentics.mesh.unhibernate.ANSIJoinFragment;

/**
 * Wrapper for Hibernate {@link ANSIJoinFragment} to use in SQL native filtering in Mesh.
 * 
 * @author plyhun
 *
 */
public class HibernateFilterAnsiJoinFragment extends ANSIJoinFragment implements NativeFilterJoin {

	@Override
	public String toSqlJoinString() {
		return toFromFragmentString();
	}
}
