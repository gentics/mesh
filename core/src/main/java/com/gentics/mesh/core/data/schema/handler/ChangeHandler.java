package com.gentics.mesh.core.data.schema.handler;

import com.gentics.mesh.core.data.schema.SchemaChange;
import com.gentics.mesh.core.rest.schema.change.impl.ChangeMigrationReport;

import rx.Observable;

/**
 * A change handler is a handler which can handle {@link SchemaChange} objects. Typical handlers implement rename, add, remove and other operations.
 */
public interface ChangeHandler {

	/**
	 * Handle the change.
	 * 
	 * @param change
	 *            Change to be handled
	 * @param dryRun
	 * @return Observable which yields a {@link ChangeMigrationReport}
	 * 
	 */
	Observable<ChangeMigrationReport> handle(SchemaChange change, boolean dryRun);
}
