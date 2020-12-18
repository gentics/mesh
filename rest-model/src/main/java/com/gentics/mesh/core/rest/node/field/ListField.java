package com.gentics.mesh.core.rest.node.field;

import java.util.List;

/**
 * Node field definition for fields lists (e.g. node list, string list).
 * 
 * @param <T> Type of the list item
 */
public interface ListField<T extends ListableField> extends Field, MicroschemaListableField {

	/**
	 * Return the list with items.
	 * 
	 * @return
	 */
	List<T> getItems();

	@Override
	default Object getValue() {
		return getItems();
	}
}
