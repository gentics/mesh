package com.gentics.mesh.core.rest.node.field;

import java.util.List;

/**
 * Node field definition for fields lists (e.g. node list, string list).
 * 
 * @param <T>
 */
public interface ListField<T extends ListableField> extends Field, MicroschemaListableField {

	List<T> getItems();

	@Override
	default Object getValue() {
		return getItems();
	}
}
