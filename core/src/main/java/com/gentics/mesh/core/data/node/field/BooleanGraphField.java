package com.gentics.mesh.core.data.node.field;

import com.gentics.mesh.core.data.node.field.nesting.ListableGraphField;
import com.gentics.mesh.core.rest.node.field.BooleanField;

/**
 * The BooleanField Domain Model interface.
 * 
 * A boolean graph field is a basic node field which can be used to store boolean values.
 */
public interface BooleanGraphField extends ListableGraphField, BasicGraphField<BooleanField> {

	FieldTransformator<BooleanField> BOOLEAN_TRANSFORMATOR = (container, ac, fieldKey, fieldSchema, languageTags, level, parentNode) -> {
		BooleanGraphField graphBooleanField = container.getBoolean(fieldKey);
		if (graphBooleanField == null) {
			return null;
		} else {
			return graphBooleanField.transformToRest(ac);
		}
	};

	FieldUpdater BOOLEAN_UPDATER = (container, ac, fieldMap, fieldKey, fieldSchema, schema) -> {
		BooleanGraphField booleanGraphField = container.getBoolean(fieldKey);
		BooleanField booleanField = fieldMap.getBooleanField(fieldKey);
		boolean isBooleanFieldSetToNull = fieldMap.hasField(fieldKey) && (booleanField == null || booleanField.getValue() == null);
		GraphField.failOnDeletionOfRequiredField(booleanGraphField, isBooleanFieldSetToNull, fieldSchema, fieldKey, schema);
		boolean restIsNullOrEmpty = booleanField == null || booleanField.getValue() == null;
		GraphField.failOnMissingRequiredField(booleanGraphField, restIsNullOrEmpty, fieldSchema, fieldKey, schema);

		// Handle deletion
		if (isBooleanFieldSetToNull && booleanGraphField != null) {
			booleanGraphField.removeField(container);
			return;
		}

		// Rest model is empty or null - Abort
		if (restIsNullOrEmpty) {
			return;
		}

		// Handle Update / Create
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
