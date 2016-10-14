package com.gentics.mesh.core.field.node;

import com.gentics.mesh.core.field.DataProvider;
import com.gentics.mesh.core.field.FieldFetcher;
import com.gentics.mesh.test.TestDataProvider;

public interface NodeFieldTestHelper {

	static final DataProvider FILL = (container, name) -> {
		container.createNode(name, TestDataProvider.getInstance().getFolder("2015"));
	};

	static final DataProvider CREATE_EMPTY = (container, name) -> {
	};

	static final FieldFetcher FETCH = (container, name) -> container.getNode(name);

}
