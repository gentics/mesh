package com.gentics.mesh.core.data.node.field.list;

import com.gentics.mesh.core.data.node.field.nesting.HibListableField;
import com.gentics.mesh.core.rest.node.field.Field;

/**
 * An extension for {@link HibListField} with the items being the references to another entities, rather than primitives/strings.
 * 
 * @author plyhun
 *
 * @param <T>
 * @param <RM>
 * @param <U>
 */
public interface HibReferencingListField<T extends HibListableField, RM extends Field, U> extends HibListField<T, RM, U> {

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
