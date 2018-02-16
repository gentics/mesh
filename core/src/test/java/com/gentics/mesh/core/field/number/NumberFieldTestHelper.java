package com.gentics.mesh.core.field.number;

import com.gentics.mesh.core.data.GraphFieldContainer;
import com.gentics.mesh.core.field.DataProvider;
import com.gentics.mesh.core.field.FieldFetcher;

public interface NumberFieldTestHelper {

	int NUMBERVALUE = 4711;

	DataProvider FILL = (container, name) -> container.createNumber(name).setNumber(NUMBERVALUE);

	DataProvider FILL1 = (container, name) -> container.createNumber(name).setNumber(1L);

	DataProvider FILL0 = (container, name) -> container.createNumber(name).setNumber(0L);

	DataProvider CREATE_EMPTY = (container, name) -> container.createNumber(name).setNumber(null);

	FieldFetcher FETCH = GraphFieldContainer::getNumber;

}
