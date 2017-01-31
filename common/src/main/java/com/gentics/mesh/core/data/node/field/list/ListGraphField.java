package com.gentics.mesh.core.data.node.field.list;

import java.util.List;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.node.field.nesting.ListableGraphField;
import com.gentics.mesh.core.data.node.field.nesting.MicroschemaListableGraphField;
import com.gentics.mesh.core.rest.node.field.Field;

/**
 * A listable graph field is a field which can be nested in lists. Typical listable graph fields are date, string, number fields.
 * 
 * @param <T>
 * @param <RM>
 * @param <U>
 */
public interface ListGraphField<T extends ListableGraphField, RM extends Field, U> extends MicroschemaListableGraphField, MeshVertex {

	/**
	 * Return the items of the list.
	 * 
	 * @return
	 */
	List<? extends T> getList();

	/**
	 * Return the values stored in the items.
	 *
	 * @return
	 */
	List<U> getValues();

	/**
	 * Return the current size of the list.
	 * 
	 * @return
	 */
	long getSize();

	/**
	 * Add the given item to the list.
	 * 
	 * @param item
	 */
	void addItem(T item);

	/**
	 * Remove the given item from the list.
	 * 
	 * @param item
	 */
	void removeItem(T item);

	/**
	 * Remove all items from the list.
	 */
	void removeAll();

	/**
	 * Return the list type.
	 * 
	 * @return
	 */
	Class<? extends T> getListType();

	/**
	 * Transform the list to the rest model.
	 * 
	 * @param ac
	 * @param fieldKey
	 * @param languageTags
	 *            list of language tags
	 * @param level
	 *            Level of transformation
	 */
	RM transformToRest(InternalActionContext ac, String fieldKey, List<String> languageTags, int level);

}