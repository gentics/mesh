package com.gentics.mesh.core.field.number;

import com.gentics.mesh.core.data.node.field.list.NumberGraphFieldList;
import com.gentics.mesh.core.field.DataProvider;
import com.gentics.mesh.core.field.FieldFetcher;

/**
 * Test helper for number fields.
 */
public interface NumberListFieldTestHelper {

	static final int NUMBERVALUE = 4711;

	static final int OTHERNUMBERVALUE = 8150;

	static final long ONE = 1L;

	static final long ZERO = 0L;

	static final DataProvider FILLNUMBERS = (container, name) -> {
		NumberGraphFieldList field = container.createNumberList(name);
		field.createNumber(NUMBERVALUE);
		field.createNumber(OTHERNUMBERVALUE);
	};

	static final DataProvider FILLONEZERO = (container, name) -> {
		NumberGraphFieldList field = container.createNumberList(name);
		field.createNumber(ONE);
		field.createNumber(ZERO);
	};

	static final DataProvider CREATE_EMPTY = (container, name) -> container.createNumberList(name);

	static final FieldFetcher FETCH = (container, name) -> container.getNumberList(name);

}
