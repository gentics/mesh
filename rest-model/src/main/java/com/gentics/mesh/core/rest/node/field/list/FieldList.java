package com.gentics.mesh.core.rest.node.field.list;

import java.util.List;

import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.HtmlField;

/**
 * Node field definition for field lists of a specific type.
 * 
 * @param <T>
 *            Type of listed elements (e.g {@link HtmlField})
 */
public interface FieldList<T> extends Field {

	/**
	 * Add a item to the field list.
	 * 
	 * @param field
	 */
	void add(T field);

	/**
	 * Return a list of items of the field list.
	 * 
	 * @return
	 */
	List<T> getItems();

	/**
	 * Set the field list.
	 * 
	 * @param items
	 * @return Fluent API
	 */
	FieldList<T> setItems(List<T> items);

	@Override
	default Object getValue() {
		return getItems();
	}
}
