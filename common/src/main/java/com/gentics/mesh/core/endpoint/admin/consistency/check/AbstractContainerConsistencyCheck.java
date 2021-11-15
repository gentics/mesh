package com.gentics.mesh.core.endpoint.admin.consistency.check;

import com.gentics.mesh.core.data.HibElement;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.endpoint.admin.consistency.ConsistencyCheckResult;

public abstract class AbstractContainerConsistencyCheck<T extends HibElement> implements CommonConsistencyCheck {

	@Override
	public ConsistencyCheckResult invoke(Database db, Tx tx, boolean attemptRepair) {
		return processForType(db, getContainerClass(), (container, result) -> {
			checkContainer(db, container, result, attemptRepair);
		}, attemptRepair, tx);
	}

	/**
	 * Get container domain class.
	 * 
	 * @return
	 */
	protected abstract Class<? extends T> getContainerClass();

	/**
	 * Run check on a container.
	 * @param db 
	 * 
	 * @param db
	 * @param microschema
	 * @param result
	 * @param attemptRepair 
	 */
	protected abstract void checkContainer(Database db, T container, ConsistencyCheckResult result, boolean attemptRepair); 
}
