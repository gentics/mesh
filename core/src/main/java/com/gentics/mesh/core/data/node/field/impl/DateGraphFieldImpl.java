package com.gentics.mesh.core.data.node.field.impl;

import static com.gentics.mesh.util.DateUtils.fromISO8601;
import static com.gentics.mesh.util.DateUtils.toISO8601;

import com.gentics.madl.wrapper.element.WrappedVertex;
import com.gentics.mesh.core.data.GraphFieldContainer;
import com.gentics.mesh.core.data.node.field.AbstractBasicField;
import com.gentics.mesh.core.data.node.field.DateGraphField;
import com.gentics.mesh.core.data.node.field.FieldGetter;
import com.gentics.mesh.core.data.node.field.FieldTransformer;
import com.gentics.mesh.core.data.node.field.FieldUpdater;
import com.gentics.mesh.core.data.node.field.GraphField;
import com.gentics.mesh.core.rest.node.field.DateField;
import com.gentics.mesh.core.rest.node.field.impl.DateFieldImpl;
import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.util.CompareUtils;

/**
 * @see DateGraphField
 */
public class DateGraphFieldImpl extends AbstractBasicField<DateField> implements DateGraphField {

	public static FieldTransformer<DateField> DATE_TRANSFORMER = (container, ac, fieldKey, fieldSchema, languageTags, level, parentNode) -> {
		DateGraphField graphDateField = container.getDate(fieldKey);
		if (graphDateField == null) {
			return null;
		} else {
			return graphDateField.transformToRest(ac);
		}
	};

	public static FieldUpdater DATE_UPDATER = (container, ac, fieldMap, fieldKey, fieldSchema, schema) -> {
		DateGraphField dateGraphField = container.getDate(fieldKey);
		DateField dateField = fieldMap.getDateField(fieldKey);
		boolean isDateFieldSetToNull = fieldMap.hasField(fieldKey) && (dateField == null || dateField.getDate() == null);
		GraphField.failOnDeletionOfRequiredField(dateGraphField, isDateFieldSetToNull, fieldSchema, fieldKey, schema);
		boolean restIsNullOrEmpty = dateField == null || dateField.getDate() == null;

		// Skip this check for no migrations
		if (!ac.isMigrationContext()) {
			GraphField.failOnMissingRequiredField(dateGraphField, restIsNullOrEmpty, fieldSchema, fieldKey, schema);
		}

		// Handle Deletion - The field was explicitly set to null and is currently set in the graph so we can remove the field from the given container
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
			container.createDate(fieldKey).setDate(fromISO8601(dateField.getDate(), true));
		} else {
			dateGraphField.setDate(fromISO8601(dateField.getDate(), true));
		}
	};

	public static FieldGetter DATE_GETTER = (container, fieldSchema) -> {
		return container.getDate(fieldSchema.getName());
	};

	public DateGraphFieldImpl(String fieldKey, WrappedVertex parentContainer) {
		super(fieldKey, parentContainer);
	}

	@Override
	public void setDate(Long date) {
		if (date == null) {
			setFieldProperty("date", null);
		} else {
			setFieldProperty("date", String.valueOf(date));
		}
	}

	@Override
	public Long getDate() {
		String value = getFieldProperty("date");
		if (value == null) {
			return null;
		} else {
			return Long.valueOf(value);
		}
	}

	@Override
	public DateField transformToRest(ActionContext ac) {
		DateField dateField = new DateFieldImpl();
		dateField.setDate(toISO8601(getDate()));
		return dateField;
	}

	@Override
	public void removeField(GraphFieldContainer container) {
		setFieldProperty("date", null);
		setFieldKey(null);
	}

	@Override
	public GraphField cloneTo(GraphFieldContainer container) {
		DateGraphField clone = container.createDate(getFieldKey());
		clone.setDate(getDate());
		return clone;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof DateGraphField) {
			Long dateA = getDate();
			Long dateB = ((DateGraphField) obj).getDate();
			return CompareUtils.equals(dateA, dateB);
		}
		if (obj instanceof DateField) {
			Long dateA = getDate();
			Long dateB = fromISO8601(((DateField) obj).getDate());
			return CompareUtils.equals(dateA, dateB);
		}
		return false;
	}
}
