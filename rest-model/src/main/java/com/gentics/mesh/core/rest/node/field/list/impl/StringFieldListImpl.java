package com.gentics.mesh.core.rest.node.field.list.impl;

import java.util.Arrays;

public class StringFieldListImpl extends AbstractFieldList<String> {
	/**
	 * Creates a string field list.
	 *
	 * @param values
	 * @return
	 */
	public static StringFieldListImpl of(String... values) {
		StringFieldListImpl stringFieldList = new StringFieldListImpl();
		stringFieldList.setItems(Arrays.asList(values));
		return stringFieldList;
	}
}