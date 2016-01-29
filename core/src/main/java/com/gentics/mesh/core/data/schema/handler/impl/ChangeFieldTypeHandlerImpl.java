package com.gentics.mesh.core.data.schema.handler.impl;

import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.schema.SchemaChange;
import com.gentics.mesh.core.data.schema.handler.AbstractChangeHandler;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeReport;

import rx.Observable;

/**
 * Handler implementation which is used to change the type of a field.
 * 
 * Effects:
 * <ul>
 * <li></li>
 * </ul>
 */
@Component
public class ChangeFieldTypeHandlerImpl extends AbstractChangeHandler {

	private final static String OPERATION_NAME = "changeFieldType";

	protected String getOperation() {
		return OPERATION_NAME;
	}

	@Override
	public Observable<SchemaChangeReport> handle(SchemaChange change, boolean dryRun) {

		// 1. get old schema
		// 2. iterate over all affected nodes
		// 3. iterate over all languages of the node
		// 4. determine whether the node already has the field that should be changed
		// has field: 
		//   * create new version of node
		//   * create new field with new type and put data from old field in it use shortcut where possible (eg. just change graph property?)
		//   * add SQB to update the search index
		// has no field: check next language

		return null;
	}

}
