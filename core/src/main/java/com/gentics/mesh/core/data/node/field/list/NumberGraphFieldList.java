package com.gentics.mesh.core.data.node.field.list;

import com.gentics.mesh.core.data.node.field.FieldTransformator;
import com.gentics.mesh.core.data.node.field.NumberGraphField;
import com.gentics.mesh.core.rest.node.field.list.impl.NumberFieldListImpl;

import rx.Observable;

public interface NumberGraphFieldList extends ListGraphField<NumberGraphField, NumberFieldListImpl, Number> {

	String TYPE = "number";

	FieldTransformator NUMBER_LIST_TRANSFORMATOR = (container, ac, fieldKey, fieldSchema, languageTags, level, parentNode) -> {
		NumberGraphFieldList numberFieldList = container.getNumberList(fieldKey);
		if (numberFieldList == null) {
			return Observable.just(new NumberFieldListImpl());
		} else {
			return numberFieldList.transformToRest(ac, fieldKey, languageTags, level);
		}
	};

	/**
	 * Create a new number graph field with the given value.
	 * 
	 * @param value
	 * @return
	 */
	NumberGraphField createNumber(Number value);

	/**
	 * Return the graph number field at the given position.
	 * 
	 * @param index
	 * @return
	 */
	NumberGraphField getNumber(int index);

}
