package com.gentics.mesh.core.data.node.field;

import com.gentics.mesh.core.data.node.field.nesting.ListableGraphField;
import com.gentics.mesh.core.rest.node.field.NumberField;
import com.gentics.mesh.core.rest.node.field.impl.NumberFieldImpl;

import rx.Observable;

/**
 * The NumberField Domain Model interface.
 * 
 * A number graph field is a basic node field which can be used to store number values.
 */
public interface NumberGraphField extends ListableGraphField, BasicGraphField<NumberField> {

	FieldTransformator NUMBER_TRANSFORMATOR = (container, ac, fieldKey, fieldSchema, languageTags, level, parentNode) -> {
		NumberGraphField graphNumberField = container.getNumber(fieldKey);
		if (graphNumberField == null) {
			return Observable.just(new NumberFieldImpl());
		} else {
			return graphNumberField.transformToRest(ac);
		}
	};
	FieldUpdater  NUMBER_UPDATER = (container, ac, fieldMap, fieldKey, fieldSchema, schema) -> {
		NumberGraphField numberGraphField = container.getNumber(fieldKey);
		NumberField numberField = fieldMap.getNumberField(fieldKey);
		boolean isNumberFieldSetToNull = fieldMap.hasField(fieldKey) && (numberField == null || numberField.getNumber() == null);
		GraphField.failOnDeletionOfRequiredField(numberGraphField, isNumberFieldSetToNull, fieldSchema, fieldKey, schema);
		GraphField.failOnMissingRequiredField(numberGraphField, numberField == null || numberField.getNumber() == null, fieldSchema, fieldKey, schema);
		if (numberField == null) {
			return;
		}
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
