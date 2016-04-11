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
	FieldUpdater  NUMBER_UPDATER = (container, ac, fieldKey, restField, fieldSchema, schema) -> {
		NumberGraphField numberGraphField = container.getNumber(fieldKey);
		GraphField.failOnMissingMandatoryField(ac, numberGraphField, restField, fieldSchema, fieldKey, schema);
		NumberField numberField = (NumberFieldImpl) restField;
		if (restField == null) {
			return;
		}
		if (numberGraphField == null) {
			container.createNumber(fieldKey).setNumber(numberField.getNumber());
		} else {
			numberGraphField.setNumber(numberField.getNumber());
		}
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
