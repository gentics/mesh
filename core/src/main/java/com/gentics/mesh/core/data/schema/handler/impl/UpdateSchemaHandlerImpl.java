package com.gentics.mesh.core.data.schema.handler.impl;

import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.schema.SchemaChange;
import com.gentics.mesh.core.data.schema.handler.AbstractChangeHandler;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeReport;

import rx.Observable;

/**
 * Handler implementation which is used to update schema specific flags. (eg. displayField, segmentField, container flag)
 */
@Component
public class UpdateSchemaHandlerImpl extends AbstractChangeHandler {

	private final static String OPERATION_NAME = "updateSchema";

	@Override
	protected String getOperation() {
		return OPERATION_NAME;
	}

	@Override
	public Observable<SchemaChangeReport> handle(SchemaChange change, boolean dryRun) {
		// displayField:
		// 1. Iterate over all affected node
		// Check whether the field exists for all nodes since the displayField requires the field to be mandatory.
		// TODO should we maybe define a default value?
		
		// segmentField:
		//1. Iterate over all affected node
		//2. Check whether the new segment field would cause webroot path segment conflicts 

		// container Flag: true -> false
		// 1. Iterate over all affected node
		// 2. Check whether the node has children
		// has Children: 
		//  * Create new version of node
		//  * Remove all children edges from new version
		// has no Children:
		//  * continue with next node
		
		// container Flag: false -> true
		// 1. Iterate over all affected nodes
		// 2. Check whether the node has children
		
		
		// name?
		
		// description?
		return null;
	}

}
