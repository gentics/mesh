package com.gentics.mesh.core.data.node.field.impl;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.core.data.GraphFieldContainer;
import com.gentics.mesh.core.data.HibField;
import com.gentics.mesh.core.data.HibFieldContainer;
import com.gentics.mesh.core.data.node.field.AbstractBasicField;
import com.gentics.mesh.core.data.node.field.BooleanGraphField;
import com.gentics.mesh.core.data.node.field.FieldGetter;
import com.gentics.mesh.core.data.node.field.FieldTransformer;
import com.gentics.mesh.core.data.node.field.FieldUpdater;
import com.gentics.mesh.core.data.node.field.GraphField;
import com.gentics.mesh.core.data.node.field.HibBooleanField;
import com.gentics.mesh.core.rest.node.field.BooleanField;
import com.gentics.mesh.core.rest.node.field.impl.BooleanFieldImpl;
import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.util.CompareUtils;
import com.syncleus.ferma.AbstractVertexFrame;

/**
 * @see BooleanGraphField
 */
public class BooleanGraphFieldImpl extends AbstractBasicField<BooleanField> implements BooleanGraphField {

	public static FieldTransformer<BooleanField> BOOLEAN_TRANSFORMER = (container, ac, fieldKey, fieldSchema, languageTags, level,
			parentNode) -> {
		HibBooleanField graphBooleanField = container.getBoolean(fieldKey);
		if (graphBooleanField == null) {
			return null;
		} else {
			return graphBooleanField.transformToRest(ac);
		}
	};

	public static FieldUpdater BOOLEAN_UPDATER = (container, ac, fieldMap, fieldKey, fieldSchema, schema) -> {
		HibBooleanField booleanGraphField = container.getBoolean(fieldKey);
		BooleanField booleanField = fieldMap.getBooleanField(fieldKey);
		boolean isBooleanFieldSetToNull = fieldMap.hasField(fieldKey) && (booleanField == null || booleanField.getValue() == null);
		HibField.failOnDeletionOfRequiredField(booleanGraphField, isBooleanFieldSetToNull, fieldSchema, fieldKey, schema);
		boolean restIsNullOrEmpty = booleanField == null || booleanField.getValue() == null;

		// Skip this check for no migrations
		if (!ac.isMigrationContext()) {
			HibField.failOnMissingRequiredField(booleanGraphField, restIsNullOrEmpty, fieldSchema, fieldKey, schema);
		}

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

	public static FieldGetter BOOLEAN_GETTER = (container, fieldSchema) -> {
		return container.getBoolean(fieldSchema.getName());
	};

	public BooleanGraphFieldImpl(String fieldKey, AbstractVertexFrame parentContainer) {
		super(fieldKey, parentContainer);
	}

	@Override
	public void setBoolean(Boolean bool) {
		if (bool == null) {
			setFieldProperty("boolean", null);
		} else {
			setFieldProperty("boolean", String.valueOf(bool));
		}
	}

	@Override
	public Boolean getBoolean() {
		String fieldValue = getFieldProperty("boolean");
		if (fieldValue == null || fieldValue.equals("null")) {
			return null;
		}
		return Boolean.valueOf(fieldValue);
	}

	@Override
	public BooleanField transformToRest(ActionContext ac) {
		BooleanFieldImpl restModel = new BooleanFieldImpl();
		restModel.setValue(getBoolean());
		return restModel;
	}

	@Override
	public void removeField(BulkActionContext bac, HibFieldContainer container) {
		setFieldProperty("boolean", null);
		setFieldKey(null);
	}

	@Override
	public HibBooleanField cloneTo(HibFieldContainer container) {
		HibBooleanField clone = container.createBoolean(getFieldKey());
		clone.setBoolean(getBoolean());
		return clone;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof BooleanGraphField) {
			Boolean valueA = getBoolean();
			Boolean valueB = ((BooleanGraphField) obj).getBoolean();
			return CompareUtils.equals(valueA, valueB);
		}
		if (obj instanceof BooleanField) {
			Boolean valueA = getBoolean();
			Boolean valueB = ((BooleanField) obj).getValue();
			return CompareUtils.equals(valueA, valueB);
		}
		return false;
	}
}
