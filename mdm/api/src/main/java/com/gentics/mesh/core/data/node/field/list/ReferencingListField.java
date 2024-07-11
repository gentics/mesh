package com.gentics.mesh.core.data.node.field.list;

import com.gentics.mesh.core.data.node.field.nesting.ListableField;
import com.gentics.mesh.core.rest.node.field.FieldModel;

/**
 * An extension for {@link ListField} with the items being the references to another entities, rather than primitives/strings.
 * 
 * @author plyhun
 *
 * @param <T>
 * @param <RM>
 * @param <U>
 */
public interface ReferencingListField<T extends ListableField, RM extends FieldModel, U> extends ListField<T, RM, U> {

	/**
	 * Insert a new reference item upon the given index.
	 * 
	 * @param index
	 * @param item
	 */
	void insertReferenced(int index, U item);

	/**
	 * Delete the referenced item;
	 * 
	 * @param item
	 */
	void deleteReferenced(U item);
}
