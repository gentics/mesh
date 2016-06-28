package com.gentics.mesh.core.data.node.field.list;

import com.gentics.mesh.core.data.node.field.FieldGetter;
import com.gentics.mesh.core.data.node.field.FieldTransformator;
import com.gentics.mesh.core.data.node.field.FieldUpdater;
import com.gentics.mesh.core.data.node.field.GraphField;
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

	FieldUpdater NUMBER_LIST_UPDATER = (container, ac, fieldMap, fieldKey, fieldSchema, schema) -> {
		NumberFieldListImpl numberList = fieldMap.getNumberFieldList(fieldKey);

		NumberGraphFieldList graphNumberFieldList = container.getNumberList(fieldKey);
		boolean isNumberListFieldSetToNull = fieldMap.hasField(fieldKey) && numberList == null;
		GraphField.failOnDeletionOfRequiredField(graphNumberFieldList, isNumberListFieldSetToNull, fieldSchema, fieldKey, schema);
		boolean restIsNullOrEmpty = numberList == null ||  numberList.getItems().isEmpty();
		GraphField.failOnMissingRequiredField(graphNumberFieldList, restIsNullOrEmpty, fieldSchema, fieldKey, schema);

		// Handle Deletion
		if (isNumberListFieldSetToNull && graphNumberFieldList != null) {
			graphNumberFieldList.removeField(container);
			return;
		}

		// Rest model is empty or null - Abort
		if (restIsNullOrEmpty) {
			return;
		}

		// Handle Create
		if (graphNumberFieldList == null) {
			graphNumberFieldList = container.createNumberList(fieldKey);
		}

		// Handle Update
		graphNumberFieldList.removeAll();
		for (
	Number item:numberList.getItems())
	{
		graphNumberFieldList.createNumber(item);
	}

	};

	FieldGetter NUMBER_LIST_GETTER = (container, fieldSchema) -> {
		return container.getNumberList(fieldSchema.getName());
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
