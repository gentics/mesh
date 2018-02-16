package com.gentics.mesh.core.field.bool;

import com.gentics.mesh.core.data.GraphFieldContainer;
import com.gentics.mesh.core.data.node.field.list.BooleanGraphFieldList;
import com.gentics.mesh.core.field.DataProvider;
import com.gentics.mesh.core.field.FieldFetcher;

public interface BooleanListFieldHelper {
	DataProvider FILL = (container, name) -> {
		BooleanGraphFieldList field = container.createBooleanList(name);
		field.createBoolean(true);
		field.createBoolean(false);
	};

	DataProvider CREATE_EMPTY = GraphFieldContainer::createBooleanList;

	FieldFetcher FETCH = GraphFieldContainer::getBooleanList;

}
