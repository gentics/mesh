package com.gentics.mesh.core.field.string;

import com.gentics.mesh.core.data.GraphFieldContainer;
import com.gentics.mesh.core.data.node.field.list.StringGraphFieldList;
import com.gentics.mesh.core.field.DataProvider;
import com.gentics.mesh.core.field.FieldFetcher;

public interface StringListFieldTestHelper {

	String TEXT1 = "one";

	String TEXT2 = "two";

	String TEXT3 = "three";

	DataProvider FILLTEXT = (container, name) -> {
		StringGraphFieldList field = container.createStringList(name);
		field.createString(TEXT1);
		field.createString(TEXT2);
		field.createString(TEXT3);
	};

	DataProvider FILLNUMBERS = (container, name) -> {
		StringGraphFieldList field = container.createStringList(name);
		field.createString("1");
		field.createString("0");
	};

	DataProvider FILLTRUEFALSE = (container, name) -> {
		StringGraphFieldList field = container.createStringList(name);
		field.createString("true");
		field.createString("false");
	};

	DataProvider CREATE_EMPTY = GraphFieldContainer::createStringList;

	FieldFetcher FETCH = GraphFieldContainer::getStringList;

}
