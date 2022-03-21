package com.gentics.mesh.core.endpoint.admin.consistency;

import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.db.Tx;

/**
 * A consistency check must identify and log database inconsistencies.
 */
public interface ConsistencyCheck {

	/**
	 * Invoke the consistency check and return the result.
	 * 
	 * @param db
	 *            database
	 * @param tx
	 *            current transaction
	 * @param attemptRepair
	 * @return Result of the consistency check
	 */
	ConsistencyCheckResult invoke(Database db, Tx tx, boolean attemptRepair);

	/**
	 * Return the public name of the check.
	 * 
	 * @return
	 */
	String getName();
}
