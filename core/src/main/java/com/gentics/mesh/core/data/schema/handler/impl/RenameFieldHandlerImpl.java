package com.gentics.mesh.core.data.schema.handler.impl;

import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.schema.SchemaChange;
import com.gentics.mesh.core.data.schema.handler.AbstractChangeHandler;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeReport;

import rx.Observable;

/**
 * Handler implementation which is used to rename fields.
 */
@Component
public class RenameFieldHandlerImpl extends AbstractChangeHandler {

	private final static String OPERATION_NAME = "renameField";

	protected String getOperation() {
		return OPERATION_NAME;
	}

	@Override
	public Observable<SchemaChangeReport> handle(SchemaChange change, boolean dryRun) {
		// 1. get old schema
		// 2. iterate over all affected nodes
		// 3. iterate over all languages of the node
		// 4. Check whether the language already got the field
		// has field: 
		// * Create new version of the node
		// * Rename the field key
		// * Add SQB for search index update
		// has no field: Check next language		
		return null;
	}

}
