package com.gentics.mesh.core.field.json;

import static com.gentics.mesh.core.field.json.JsonFieldTestHelper.make;

import com.gentics.mesh.core.data.node.field.list.HibJsonFieldList;
import com.gentics.mesh.core.field.DataProvider;
import com.gentics.mesh.core.field.FieldFetcher;

public interface JsonListFieldTestHelper {

	static final String TEXT1 = "one";

	static final String TEXT2 = "two";

	static final String TEXT3 = "three";

	static final DataProvider FILLTEXT = (container, name) -> {
		HibJsonFieldList field = container.createJsonList(name);
		field.createJson(make(TEXT1));
		field.createJson(make(TEXT2));
		field.createJson(make(TEXT3));
	};

	static final DataProvider FILLNUMBERS = (container, name) -> {
		HibJsonFieldList field = container.createJsonList(name);
		field.createJson(make("1"));
		field.createJson(make("0"));
	};

	static final DataProvider FILLTRUEFALSE = (container, name) -> {
		HibJsonFieldList field = container.createJsonList(name);
		field.createJson(make("true"));
		field.createJson(make("false"));
	};

	static final DataProvider CREATE_EMPTY = (container, name) -> container.createJsonList(name);

	static final FieldFetcher FETCH = (container, name) -> container.getJsonList(name);

}
