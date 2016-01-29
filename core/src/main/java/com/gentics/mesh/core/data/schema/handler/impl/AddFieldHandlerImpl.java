package com.gentics.mesh.core.data.schema.handler.impl;

import com.gentics.mesh.core.data.schema.SchemaChange;
import com.gentics.mesh.core.data.schema.handler.AbstractChangeHandler;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeReport;

import rx.Observable;

public class AddFieldHandlerImpl extends AbstractChangeHandler {

	private final static String OPERATION_NAME = "addField";

	protected String getOperation() {
		return OPERATION_NAME;
	}

	@Override
	public Observable<SchemaChangeReport> handle(SchemaChange change, boolean dryRun) {
		// No checks needed? 
		// Add SQB for all affected nodes. This way the Search index contains the empty property? 
		return null;
	}

}
