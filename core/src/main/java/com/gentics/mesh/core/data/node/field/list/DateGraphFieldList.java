package com.gentics.mesh.core.data.node.field.list;

import com.gentics.mesh.core.data.node.field.DateGraphField;
import com.gentics.mesh.core.data.node.field.FieldGetter;
import com.gentics.mesh.core.data.node.field.FieldTransformator;
import com.gentics.mesh.core.data.node.field.FieldUpdater;
import com.gentics.mesh.core.data.node.field.GraphField;
import com.gentics.mesh.core.rest.node.field.list.impl.DateFieldListImpl;

import rx.Observable;

public interface DateGraphFieldList extends ListGraphField<DateGraphField, DateFieldListImpl, Long> {

	String TYPE = "date";

	FieldTransformator DATE_LIST_TRANSFORMATOR = (container, ac, fieldKey, fieldSchema, languageTags, level, parentNode) -> {
		DateGraphFieldList dateFieldList = container.getDateList(fieldKey);
		if (dateFieldList == null) {
			return Observable.just(null);
		} else {
			return dateFieldList.transformToRest(ac, fieldKey, languageTags, level);
		}
	};

	FieldUpdater DATE_LIST_UPDATER = (container, ac, fieldMap, fieldKey, fieldSchema, schema) -> {
		DateGraphFieldList graphDateFieldList = container.getDateList(fieldKey);
		DateFieldListImpl dateList = fieldMap.getDateFieldList(fieldKey);
		boolean isDateListFieldSetToNull = fieldMap.hasField(fieldKey) && (dateList == null);
		GraphField.failOnDeletionOfRequiredField(graphDateFieldList, isDateListFieldSetToNull, fieldSchema, fieldKey, schema);
		boolean restIsNull = dateList == null;
		GraphField.failOnMissingRequiredField(graphDateFieldList, restIsNull, fieldSchema, fieldKey, schema);

		// Handle Deletion
		if (isDateListFieldSetToNull && graphDateFieldList != null) {
			graphDateFieldList.removeField(container);
			return;
		}

		// Rest model is empty or null - Abort
		if (restIsNull) {
			return;
		}

		// Always create a new list. 
		// This will effectively unlink the old list and create a new one. 
		// Otherwise the list which is linked to old versions would be updated. 
		graphDateFieldList = container.createDateList(fieldKey);

		// Handle Update
		graphDateFieldList.removeAll();
		for (Long item : dateList.getItems()) {
			graphDateFieldList.createDate(item);
		}

	};

	FieldGetter DATE_LIST_GETTER = (container, fieldSchema) -> {
		return container.getDateList(fieldSchema.getName());
	};

	/**
	 * Add another graph field to the list of graph fields.
	 * 
	 * @param date
	 *            Date to be set for the new field
	 * @return
	 */
	DateGraphField createDate(Long date);

	/**
	 * Return the date field at the given index of the list.
	 * 
	 * @param index
	 * @return
	 */
	DateGraphField getDate(int index);
}
