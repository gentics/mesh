package com.gentics.mesh.core.field.bool;

import com.gentics.mesh.core.data.GraphFieldContainer;
import com.gentics.mesh.core.field.DataProvider;
import com.gentics.mesh.core.field.FieldFetcher;

public interface BooleanFieldTestHelper {

	DataProvider FILLTRUE = (container, name) -> container.createBoolean(name).setBoolean(true);
	DataProvider FILLFALSE = (container, name) -> container.createBoolean(name).setBoolean(false);
	DataProvider CREATE_EMPTY = (container, name) -> container.createBoolean(name).setBoolean(null);
	FieldFetcher FETCH = GraphFieldContainer::getBoolean;

}
