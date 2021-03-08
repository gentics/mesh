package com.gentics.mesh.core.field.string;

import com.gentics.mesh.core.data.node.field.list.StringGraphFieldList;
import com.gentics.mesh.core.field.DataProvider;
import com.gentics.mesh.core.field.FieldFetcher;

public interface StringListFieldTestHelper {

	static final String TEXT1 = "one";

	static final String TEXT2 = "two";

	static final String TEXT3 = "three";

	static final DataProvider FILLTEXT = (container, name) -> {
		StringGraphFieldList field = container.createStringList(name);
		field.createString(TEXT1);
		field.createString(TEXT2);
		field.createString(TEXT3);
	};

	static final DataProvider FILLNUMBERS = (container, name) -> {
		StringGraphFieldList field = container.createStringList(name);
		field.createString("1");
		field.createString("0");
	};

	static final DataProvider FILLTRUEFALSE = (container, name) -> {
		StringGraphFieldList field = container.createStringList(name);
		field.createString("true");
		field.createString("false");
	};

	static final DataProvider CREATE_EMPTY = (container, name) -> container.createStringList(name);

	static final FieldFetcher FETCH = (container, name) -> container.getStringList(name);

}
