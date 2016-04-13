package com.gentics.mesh.core.data.node.field;

import com.gentics.mesh.core.data.node.field.nesting.ListableGraphField;
import com.gentics.mesh.core.rest.node.field.BooleanField;
import com.gentics.mesh.core.rest.node.field.impl.BooleanFieldImpl;

import rx.Observable;

/**
 * The BooleanField Domain Model interface.
 * 
 * A boolean graph field is a basic node field which can be used to store boolean values.
 */
public interface BooleanGraphField extends ListableGraphField, BasicGraphField<BooleanField> {

	FieldTransformator BOOLEAN_TRANSFORMATOR = (container, ac, fieldKey, fieldSchema, languageTags, level, parentNode) -> {
		BooleanGraphField graphBooleanField = container.getBoolean(fieldKey);
		if (graphBooleanField == null) {
			return Observable.just(new BooleanFieldImpl());
		} else {
			return graphBooleanField.transformToRest(ac);
		}
	};

	FieldUpdater BOOLEAN_UPDATER = (container, ac, fieldMap, fieldKey, fieldSchema, schema) -> {
		BooleanGraphField booleanGraphField = container.getBoolean(fieldKey);
		BooleanField booleanField = fieldMap.getBooleanField(fieldKey);
		boolean isBooleanFieldSetToNull = fieldMap.hasField(fieldKey) && (booleanField == null || booleanField.getValue() == null);
		GraphField.failOnDeletionOfRequiredField(booleanGraphField, isBooleanFieldSetToNull, fieldSchema, fieldKey, schema);
		GraphField.failOnMissingRequiredField(booleanGraphField, booleanField == null || booleanField.getValue() == null, fieldSchema, fieldKey, schema);
		if (booleanField == null) {
			return;
		}
		if (booleanGraphField == null) {
			container.createBoolean(fieldKey).setBoolean(booleanField.getValue());
		} else {
			booleanGraphField.setBoolean(booleanField.getValue());
		}
	};

	FieldGetter BOOLEAN_GETTER = (container, fieldSchema) -> {
		return container.getBoolean(fieldSchema.getName());
	};

	/**
	 * Return the boolean field value.
	 * 
	 * @return
	 */
	Boolean getBoolean();

	/**
	 * Set the boolean field value.
	 * 
	 * @param bool
	 */
	void setBoolean(Boolean bool);

}
