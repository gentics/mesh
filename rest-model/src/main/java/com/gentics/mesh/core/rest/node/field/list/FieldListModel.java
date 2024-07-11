package com.gentics.mesh.core.rest.node.field.list;

import java.util.List;

import com.gentics.mesh.core.rest.node.field.FieldModel;
import com.gentics.mesh.core.rest.node.field.HtmlFieldModel;

/**
 * Node field definition for field lists of a specific type.
 * 
 * @param <T>
 *            Type of listed elements (e.g {@link HtmlFieldModel})
 */
public interface FieldListModel<T> extends FieldModel {

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
	FieldListModel<T> setItems(List<T> items);

	/**
	 * Get the item type,
	 * 
	 * @return
	 */
	public String getItemType();

	@Override
	default Object getValue() {
		return getItems();
	}
}
