package com.gentics.mesh.core.field.bool;

import com.gentics.mesh.core.data.node.field.list.BooleanGraphFieldList;
import com.gentics.mesh.core.field.DataProvider;
import com.gentics.mesh.core.field.FieldFetcher;

public interface BooleanListFieldHelper {
	static final DataProvider FILL = (container, name) -> {
		BooleanGraphFieldList field = container.createBooleanList(name);
		field.createBoolean(true);
		field.createBoolean(false);
	};

	static final DataProvider CREATE_EMPTY = (container, name) -> container.createBooleanList(name);

	static final FieldFetcher FETCH = (container, name) -> container.getBooleanList(name);

}
