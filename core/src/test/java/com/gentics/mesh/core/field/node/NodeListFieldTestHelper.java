package com.gentics.mesh.core.field.node;

import com.gentics.mesh.core.data.node.field.list.HibNodeFieldList;
import com.gentics.mesh.core.field.DataProvider;
import com.gentics.mesh.core.field.FieldFetcher;
import com.gentics.mesh.test.TestDataProvider;

/**
 * Test helper for node list fields. 
 */
public interface NodeListFieldTestHelper {

	static final DataProvider FILL = (container, name) -> {
		HibNodeFieldList list = container.createNodeList(name);
		list.addItem(list.createNode("0", TestDataProvider.getInstance().getFolder("2015")));
		list.addItem(list.createNode("1", TestDataProvider.getInstance().getFolder("2014")));
		list.addItem(list.createNode("2", TestDataProvider.getInstance().getFolder("news")));
	};

	static final DataProvider CREATE_EMPTY = (container, name) -> {
	};

	static final FieldFetcher FETCH = (container, name) -> container.getNodeList(name);

}
