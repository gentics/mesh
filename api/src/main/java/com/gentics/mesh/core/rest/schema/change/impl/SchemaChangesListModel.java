package com.gentics.mesh.core.rest.schema.change.impl;

import java.util.ArrayList;
import java.util.List;

import com.gentics.mesh.core.rest.common.RestModel;

public class SchemaChangesListModel implements RestModel {

	List<SchemaChangeModel> changes = new ArrayList<>();

	public SchemaChangesListModel() {
	}

	public List<SchemaChangeModel> getChanges() {
		return changes;
	}

}
