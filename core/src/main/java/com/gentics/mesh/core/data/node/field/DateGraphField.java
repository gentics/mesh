package com.gentics.mesh.core.data.node.field;

import static com.gentics.mesh.util.DateUtils.fromISO8601;

import com.gentics.mesh.core.data.node.field.nesting.ListableGraphField;
import com.gentics.mesh.core.rest.node.field.DateField;
import com.gentics.mesh.core.rest.node.field.impl.DateFieldImpl;

/**
 * The DateField Domain Model interface.
 * 
 * A date graph field is a basic node field which can be used to store date values.
 */
public interface DateGraphField extends ListableGraphField, BasicGraphField<DateField> {

	FieldTransformator<DateField> DATE_TRANSFORMATOR = (container, ac, fieldKey, fieldSchema, languageTags, level, parentNode) -> {
		DateGraphField graphDateField = container.getDate(fieldKey);
		if (graphDateField == null) {
			return new DateFieldImpl();
		} else {
			return graphDateField.transformToRest(ac);
		}
	};

	FieldUpdater DATE_UPDATER = (container, ac, fieldMap, fieldKey, fieldSchema, schema) -> {
		DateGraphField dateGraphField = container.getDate(fieldKey);
		DateField dateField = fieldMap.getDateField(fieldKey);
		boolean isDateFieldSetToNull = fieldMap.hasField(fieldKey) && (dateField == null || dateField.getDate() == null);
		GraphField.failOnDeletionOfRequiredField(dateGraphField, isDateFieldSetToNull, fieldSchema, fieldKey, schema);
		boolean restIsNullOrEmpty = dateField == null || dateField.getDate() == null;
		GraphField.failOnMissingRequiredField(dateGraphField, restIsNullOrEmpty, fieldSchema, fieldKey, schema);

		// Handle Deletion
		if (isDateFieldSetToNull && dateGraphField != null) {
			dateGraphField.removeField(container);
			return;
		}

		// Rest model is empty or null - Abort
		if (restIsNullOrEmpty) {
			return;
		}

		// Handle Update / Create
		if (dateGraphField == null) {
			container.createDate(fieldKey).setDate(fromISO8601(dateField.getDate()));
		} else {
			dateGraphField.setDate(fromISO8601(dateField.getDate()));
		}
	};

	FieldGetter DATE_GETTER = (container, fieldSchema) -> {
		return container.getDate(fieldSchema.getName());
	};

	/**
	 * Set the date within the field.
	 * 
	 * @param date
	 */
	void setDate(Long date);

	/**
	 * Return the date which is stored in the field.
	 * 
	 * @return
	 */
	Long getDate();

}
