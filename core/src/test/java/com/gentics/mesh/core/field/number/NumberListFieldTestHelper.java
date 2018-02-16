package com.gentics.mesh.core.field.number;

import com.gentics.mesh.core.data.GraphFieldContainer;
import com.gentics.mesh.core.data.node.field.list.NumberGraphFieldList;
import com.gentics.mesh.core.field.DataProvider;
import com.gentics.mesh.core.field.FieldFetcher;

public interface NumberListFieldTestHelper {

	int NUMBERVALUE = 4711;

	int OTHERNUMBERVALUE = 8150;

	long ONE = 1L;

	long ZERO = 0L;

	DataProvider FILLNUMBERS = (container, name) -> {
		NumberGraphFieldList field = container.createNumberList(name);
		field.createNumber(NUMBERVALUE);
		field.createNumber(OTHERNUMBERVALUE);
	};

	DataProvider FILLONEZERO = (container, name) -> {
		NumberGraphFieldList field = container.createNumberList(name);
		field.createNumber(ONE);
		field.createNumber(ZERO);
	};

	DataProvider CREATE_EMPTY = GraphFieldContainer::createNumberList;

	FieldFetcher FETCH = GraphFieldContainer::getNumberList;

}
