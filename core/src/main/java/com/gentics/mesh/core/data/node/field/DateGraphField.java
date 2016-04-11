package com.gentics.mesh.core.data.node.field;

import com.gentics.mesh.core.data.node.field.nesting.ListableGraphField;
import com.gentics.mesh.core.rest.node.field.DateField;
import com.gentics.mesh.core.rest.node.field.impl.DateFieldImpl;

import rx.Observable;

/**
 * The DateField Domain Model interface.
 * 
 * A date graph field is a basic node field which can be used to store date values.
 */
public interface DateGraphField extends ListableGraphField, BasicGraphField<DateField> {

	FieldTransformator DATE_TRANSFORMATOR = (container, ac, fieldKey, fieldSchema, languageTags, level, parentNode) -> {
		DateGraphField graphDateField = container.getDate(fieldKey);
		if (graphDateField == null) {
			return Observable.just(new DateFieldImpl());
		} else {
			return graphDateField.transformToRest(ac);
		}
	};

	FieldUpdater DATE_UPDATER = (container, ac, fieldKey, restField, fieldSchema, schema) -> {
		DateGraphField dateGraphField = container.getDate(fieldKey);
		GraphField.failOnMissingMandatoryField(ac, dateGraphField, restField, fieldSchema, fieldKey, schema);
		DateField dateField = (DateFieldImpl) restField;
		if (restField == null) {
			return;
		}
		if (dateGraphField == null) {
			container.createDate(fieldKey).setDate(dateField.getDate());
		} else {
			dateGraphField.setDate(dateField.getDate());
		}
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
