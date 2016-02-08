package com.gentics.mesh.core.data.schema.handler.impl;

import com.gentics.mesh.core.data.schema.SchemaChange;
import com.gentics.mesh.core.data.schema.handler.AbstractChangeHandler;
import com.gentics.mesh.core.rest.schema.change.impl.ChangeMigrationReport;

import rx.Observable;

public class RemoveSchemaHandlerImpl extends AbstractChangeHandler {

	private final static String OPERATION_NAME = "removeSchema";

	protected String getOperation() {
		return OPERATION_NAME;
	}

	@Override
	public Observable<ChangeMigrationReport> handle(SchemaChange change, boolean dryRun) {
		// 1. get old schema
		// 2. iterate over all affected nodes
		// 3. delete all nodes
		// 4. add SQB for each node to update search index
		return null;
	}

}
