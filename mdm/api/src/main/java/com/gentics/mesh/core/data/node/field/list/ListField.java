package com.gentics.mesh.core.data.node.field.list;

import java.util.List;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.BaseElement;
import com.gentics.mesh.core.data.Field;
import com.gentics.mesh.core.data.node.field.nesting.ListableField;
import com.gentics.mesh.core.data.node.field.nesting.MicroschemaListableField;
import com.gentics.mesh.core.rest.node.field.FieldModel;
import com.gentics.mesh.util.CompareUtils;

/**
 * A listable field is a field which can be nested in lists. Typical listable fields are date, string, number fields.
 * 
 * @param <T>
 * @param <RM>
 * @param <U>
 */
public interface ListField<T extends ListableField, RM extends FieldModel, U> extends MicroschemaListableField, TransformableListField<RM>, BaseElement {

	/**
	 * Return the items of the list.
	 * 
	 * @return
	 */
	List<? extends T> getList();

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
	int getSize();

	/**
	 * Remove all items from the list.
	 */
	void removeAll();

	@SuppressWarnings("unchecked")
	default boolean listEquals(Object obj) {
		if (getClass().isInstance(obj)) {
			List<? extends T> listA = getList();
			List<? extends T> listB = getClass().cast(obj).getList();
			return CompareUtils.equals(listA, listB);
		}
		return false;
	}

	@Override
	default void validate() {
		getList().stream().forEach(Field::validate);
	}
}
