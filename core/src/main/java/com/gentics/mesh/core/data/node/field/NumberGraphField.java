package com.gentics.mesh.core.data.node.field;

import com.gentics.mesh.core.data.node.field.nesting.ListableGraphField;
import com.gentics.mesh.core.rest.node.field.NumberField;

/**
 * The NumberField Domain Model interface.
 * 
 * A number graph field is a basic node field which can be used to store a single number value.
 */
public interface NumberGraphField extends ListableGraphField, BasicGraphField<NumberField> {

	FieldTransformator<NumberField> NUMBER_TRANSFORMATOR = (container, ac, fieldKey, fieldSchema, languageTags, level, parentNode) -> {
		NumberGraphField graphNumberField = container.getNumber(fieldKey);
		if (graphNumberField == null) {
			return null;
		} else {
			return graphNumberField.transformToRest(ac);
		}
	};

	FieldUpdater NUMBER_UPDATER = (container, ac, fieldMap, fieldKey, fieldSchema, schema) -> {
		NumberGraphField numberGraphField = container.getNumber(fieldKey);
		NumberField numberField = fieldMap.getNumberField(fieldKey);
		boolean isNumberFieldSetToNull = fieldMap.hasField(fieldKey) && (numberField == null || numberField.getNumber() == null);
		GraphField.failOnDeletionOfRequiredField(numberGraphField, isNumberFieldSetToNull, fieldSchema, fieldKey, schema);
		boolean restIsNullOrEmpty = numberField == null || numberField.getNumber() == null;
		GraphField.failOnMissingRequiredField(numberGraphField, restIsNullOrEmpty, fieldSchema, fieldKey, schema);

		// Handle Deletion
		if (isNumberFieldSetToNull && numberGraphField != null) {
			numberGraphField.removeField(container);
			return;
		}

		// Rest model is empty or null - Abort
		if (restIsNullOrEmpty) {
			return;
		}

		// Handle Update / Create
		if (numberGraphField == null) {
			container.createNumber(fieldKey).setNumber(numberField.getNumber());
		} else {
			numberGraphField.setNumber(numberField.getNumber());
		}
	};

	FieldGetter NUMBER_GETTER = (container, fieldSchema) -> {
		return container.getNumber(fieldSchema.getName());
	};

	/**
	 * Set the number in the graph field.
	 * 
	 * @param number
	 */
	public void setNumber(Number number);

	/**
	 * Return the number that is stored in the graph field.
	 * 
	 * @return
	 */
	public Number getNumber();
}
