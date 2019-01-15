package com.gentics.mesh.core.data.node.field.impl;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.core.data.GraphFieldContainer;
import com.gentics.mesh.core.data.node.field.AbstractBasicField;
import com.gentics.mesh.core.data.node.field.FieldGetter;
import com.gentics.mesh.core.data.node.field.FieldTransformer;
import com.gentics.mesh.core.data.node.field.FieldUpdater;
import com.gentics.mesh.core.data.node.field.GraphField;
import com.gentics.mesh.core.data.node.field.NumberGraphField;
import com.gentics.mesh.core.rest.node.field.NumberField;
import com.gentics.mesh.core.rest.node.field.impl.NumberFieldImpl;
import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.util.CompareUtils;
import com.syncleus.ferma.AbstractVertexFrame;

public class NumberGraphFieldImpl extends AbstractBasicField<NumberField> implements NumberGraphField {

	public static FieldTransformer<NumberField> NUMBER_TRANSFORMER = (container, ac, fieldKey, fieldSchema, languageTags, level, parentNode) -> {
		NumberGraphField graphNumberField = container.getNumber(fieldKey);
		if (graphNumberField == null) {
			return null;
		} else {
			return graphNumberField.transformToRest(ac);
		}
	};

	public static FieldUpdater NUMBER_UPDATER = (container, ac, fieldMap, fieldKey, fieldSchema, schema) -> {
		NumberGraphField numberGraphField = container.getNumber(fieldKey);
		NumberField numberField = fieldMap.getNumberField(fieldKey);
		boolean isNumberFieldSetToNull = fieldMap.hasField(fieldKey) && (numberField == null || numberField.getNumber() == null);
		GraphField.failOnDeletionOfRequiredField(numberGraphField, isNumberFieldSetToNull, fieldSchema, fieldKey, schema);
		boolean restIsNullOrEmpty = numberField == null || numberField.getNumber() == null;

		// Skip this check for no migrations
		if (!ac.isMigrationContext()) {
			GraphField.failOnMissingRequiredField(numberGraphField, restIsNullOrEmpty, fieldSchema, fieldKey, schema);
		}

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

	public static FieldGetter NUMBER_GETTER = (container, fieldSchema) -> {
		return container.getNumber(fieldSchema.getName());
	};

	public NumberGraphFieldImpl(String fieldKey, AbstractVertexFrame parentContainer) {
		super(fieldKey, parentContainer);
	}

	@Override
	public void setNumber(Number number) {
		setFieldProperty("number", number);
	}

	@Override
	public Number getNumber() {
		return getFieldProperty("number");
	}

	@Override
	public NumberField transformToRest(ActionContext ac) {
		NumberField restModel = new NumberFieldImpl();
		restModel.setNumber(getNumber());
		return restModel;
	}

	@Override
	public void removeField(BulkActionContext bac, GraphFieldContainer container) {
		setFieldProperty("number", null);
		setFieldKey(null);
	}

	@Override
	public GraphField cloneTo(GraphFieldContainer container) {
		NumberGraphField clone = container.createNumber(getFieldKey());
		clone.setNumber(getNumber());
		return clone;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof NumberGraphField) {
			Number valueA = getNumber();
			Number valueB = ((NumberGraphField) obj).getNumber();
			return CompareUtils.equals(valueA, valueB);
		}
		if (obj instanceof NumberField) {
			Number valueA = getNumber();
			Number valueB = ((NumberField) obj).getNumber();
			return CompareUtils.equals(valueA, valueB);
		}
		return false;
	}
}
