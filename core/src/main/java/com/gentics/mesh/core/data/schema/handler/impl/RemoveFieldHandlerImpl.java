package com.gentics.mesh.core.data.schema.handler.impl;

import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.schema.SchemaChange;
import com.gentics.mesh.core.data.schema.handler.AbstractChangeHandler;
import com.gentics.mesh.core.rest.schema.change.impl.ChangeMigrationReport;

import rx.Observable;

/**
 * Handler implementation which is used to remove a field from a schema.
 *
 * Effects:
 * <ul>
 * <li>Create a new node version for all nodes that are affected by this change</li>
 * <li>Delete the field all newly created node versions</li>
 * </ul>
 *
 */
@Component
public class RemoveFieldHandlerImpl extends AbstractChangeHandler {

	private final static String OPERATION_NAME = "removeField";

	protected String getOperation() {
		return OPERATION_NAME;
	}

	@Override
	public Observable<ChangeMigrationReport> handle(SchemaChange change, boolean dryRun) {
		// 1. get old schema
		// 2. iterate over all affected nodes
		// 3. iterate over all languages of the node
		// 4. Check whether the language already got the field
		// has field: 
		// * Create new version of the node
		// * Delete the field in new version
		// * Add SQB for search index update
		// has no field: Check next language
		return null;
	}

}
