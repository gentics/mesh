package com.gentics.mesh.core.field.string;

import com.gentics.mesh.core.field.DataProvider;
import com.gentics.mesh.core.field.FieldFetcher;

public interface StringFieldTestHelper {

	static final DataProvider FILLTEXT = (container, name) -> container.createString(name).setString("<b>HTML</b> content");
	static final DataProvider FILLTRUE = (container, name) -> container.createString(name).setString("true");
	static final DataProvider FILLFALSE = (container, name) -> container.createString(name).setString("false");
	static final DataProvider FILL0 = (container, name) -> container.createString(name).setString("0");
	static final DataProvider FILL1 = (container, name) -> container.createString(name).setString("1");
	static final DataProvider CREATE_EMPTY = (container, name) -> container.createString(name).setString(null);
	static final FieldFetcher FETCH = (container, name) -> container.getString(name);

}
