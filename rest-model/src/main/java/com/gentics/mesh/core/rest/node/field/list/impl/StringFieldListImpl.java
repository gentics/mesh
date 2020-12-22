package com.gentics.mesh.core.rest.node.field.list.impl;

import java.util.Arrays;

import com.gentics.mesh.core.rest.node.FieldMap;

/**
 * REST model for a string list field. Please note that {@link FieldMap} will handle the actual JSON format building.
 */
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
