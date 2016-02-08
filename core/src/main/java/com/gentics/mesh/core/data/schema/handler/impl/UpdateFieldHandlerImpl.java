package com.gentics.mesh.core.data.schema.handler.impl;

import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.schema.SchemaChange;
import com.gentics.mesh.core.data.schema.handler.AbstractChangeHandler;
import com.gentics.mesh.core.rest.schema.change.impl.ChangeMigrationReport;

import rx.Observable;

/**
 * Handler implementation which is used to update field specific settings.
 */
@Component
public class UpdateFieldHandlerImpl extends AbstractChangeHandler {

	private final static String OPERATION_NAME = "updateField";

	protected String getOperation() {
		return OPERATION_NAME;
	}

	@Override
	public Observable<ChangeMigrationReport> handle(SchemaChange change, boolean dryRun) {
		// 1. Iterate over all affected nodes
		// Check whether the new field settings would cause errors for current node
		return null;
	}

}
