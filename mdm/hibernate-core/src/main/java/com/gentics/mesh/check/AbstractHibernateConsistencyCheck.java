package com.gentics.mesh.check;

import com.gentics.mesh.core.endpoint.admin.consistency.ConsistencyCheck;

/**
 * Abstract base class for hibernate consistency checks
 */
public abstract class AbstractHibernateConsistencyCheck implements ConsistencyCheck {

	@Override
	public boolean asyncOnly() {
		return true;
	}
}
