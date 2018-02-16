package com.gentics.mesh.core.field.string;

import com.gentics.mesh.core.data.GraphFieldContainer;
import com.gentics.mesh.core.field.DataProvider;
import com.gentics.mesh.core.field.FieldFetcher;

public interface StringFieldTestHelper {

	String EXAMPLE_DATE = "2011-12-03T10:15:30Z";

	DataProvider FILLTEXT = (container, name) -> container.createString(name).setString("<b>HTML</b> content");
	DataProvider FILLTRUE = (container, name) -> container.createString(name).setString("true");
	DataProvider FILLFALSE = (container, name) -> container.createString(name).setString("false");
	DataProvider FILL0 = (container, name) -> container.createString(name).setString("0");
	DataProvider FILL1 = (container, name) -> container.createString(name).setString("1");
	DataProvider FILL_DATE = (container, name) -> container.createString(name).setString(EXAMPLE_DATE);
	DataProvider CREATE_EMPTY = (container, name) -> container.createString(name).setString(null);
	FieldFetcher FETCH = GraphFieldContainer::getString;

}
