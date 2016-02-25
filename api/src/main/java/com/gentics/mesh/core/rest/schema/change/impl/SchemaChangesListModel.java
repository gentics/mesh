package com.gentics.mesh.core.rest.schema.change.impl;

import java.util.ArrayList;
import java.util.List;

import com.gentics.mesh.core.rest.common.RestModel;

/**
 * POJO for a list of schema changes.
 */
public class SchemaChangesListModel implements RestModel {

	List<SchemaChangeModel> changes = new ArrayList<>();

	public SchemaChangesListModel() {
	}

	/**
	 * Return the list of schema changes.
	 * 
	 * @return
	 */
	public List<SchemaChangeModel> getChanges() {
		return changes;
	}

}
