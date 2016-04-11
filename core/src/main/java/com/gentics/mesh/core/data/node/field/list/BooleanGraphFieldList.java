package com.gentics.mesh.core.data.node.field.list;

import com.gentics.mesh.core.data.node.field.BooleanGraphField;
import com.gentics.mesh.core.data.node.field.FieldTransformator;
import com.gentics.mesh.core.rest.node.field.list.impl.BooleanFieldListImpl;

import rx.Observable;

public interface BooleanGraphFieldList extends ListGraphField<BooleanGraphField, BooleanFieldListImpl, Boolean> {

	String TYPE = "boolean";

	FieldTransformator BOOLEAN_LIST_TRANSFORMATOR = (container, ac, fieldKey, fieldSchema, languageTags, level, parentNode) -> {
		BooleanGraphFieldList booleanFieldList = container.getBooleanList(fieldKey);
		if (booleanFieldList == null) {
			return Observable.just(new BooleanFieldListImpl());
		} else {
			return booleanFieldList.transformToRest(ac, fieldKey, languageTags, level);
		}
	};

	/**
	 * Return the boolean graph field at index position.
	 * 
	 * @param index
	 * @return
	 */
	BooleanGraphField getBoolean(int index);

	/**
	 * Create a boolean graph field within the list.
	 * 
	 * @param flag
	 * @return
	 */
	BooleanGraphField createBoolean(Boolean flag);
}
