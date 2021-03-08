package com.gentics.mesh.core.field.number;

import com.gentics.mesh.core.field.DataProvider;
import com.gentics.mesh.core.field.FieldFetcher;

/**
 * Helper for field tests.
 */
public interface NumberFieldTestHelper {

	static final int NUMBERVALUE = 4711;

	static final DataProvider FILL = (container, name) -> container.createNumber(name).setNumber(NUMBERVALUE);

	static final DataProvider FILL1 = (container, name) -> container.createNumber(name).setNumber(1L);

	static final DataProvider FILL0 = (container, name) -> container.createNumber(name).setNumber(0L);

	static final DataProvider CREATE_EMPTY = (container, name) -> container.createNumber(name).setNumber(null);

	static final FieldFetcher FETCH = (container, name) -> container.getNumber(name);

}
