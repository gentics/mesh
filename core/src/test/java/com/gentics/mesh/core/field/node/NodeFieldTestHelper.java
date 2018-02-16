package com.gentics.mesh.core.field.node;

import com.gentics.mesh.core.data.GraphFieldContainer;
import com.gentics.mesh.core.field.DataProvider;
import com.gentics.mesh.core.field.FieldFetcher;
import com.gentics.mesh.test.TestDataProvider;

public interface NodeFieldTestHelper {

	DataProvider FILL = (container, name) -> container.createNode(name, TestDataProvider.getInstance().getFolder("2015"));

	DataProvider CREATE_EMPTY = (container, name) -> {
	};

	FieldFetcher FETCH = GraphFieldContainer::getNode;

}
