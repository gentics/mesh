package com.gentics.mesh.core.field.bool;

import com.gentics.mesh.core.data.node.field.list.BooleanFieldList;
import com.gentics.mesh.core.field.DataProvider;
import com.gentics.mesh.core.field.FieldFetcher;

public interface BooleanListFieldHelper {
	static final DataProvider FILL = (container, name) -> {
		BooleanFieldList field = container.createBooleanList(name);
		field.createBoolean(true);
		field.createBoolean(false);
	};

	static final DataProvider CREATE_EMPTY = (container, name) -> container.createBooleanList(name);

	static final FieldFetcher FETCH = (container, name) -> container.getBooleanList(name);

}
