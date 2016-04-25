package com.gentics.mesh.core.field.bool;

import com.gentics.mesh.core.field.DataProvider;
import com.gentics.mesh.core.field.FieldFetcher;

public interface BooleanFieldTestHelper {

	static final DataProvider FILLTRUE = (container, name) -> container.createBoolean(name).setBoolean(true);
	static final DataProvider FILLFALSE = (container, name) -> container.createBoolean(name).setBoolean(false);
	static final DataProvider CREATE_EMPTY = (container, name) -> container.createBoolean(name).setBoolean(null);
	static final FieldFetcher FETCH = (container, name) -> container.getBoolean(name);

}
