package com.gentics.mesh.core.data.node.field.impl;

import static com.gentics.mesh.util.DateUtils.fromISO8601;
import static com.gentics.mesh.util.DateUtils.toISO8601;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.core.data.HibField;
import com.gentics.mesh.core.data.HibFieldContainer;
import com.gentics.mesh.core.data.node.field.AbstractBasicField;
import com.gentics.mesh.core.data.node.field.DateGraphField;
import com.gentics.mesh.core.data.node.field.FieldGetter;
import com.gentics.mesh.core.data.node.field.FieldTransformer;
import com.gentics.mesh.core.data.node.field.FieldUpdater;
import com.gentics.mesh.core.data.node.field.HibDateField;
import com.gentics.mesh.core.rest.node.field.DateField;
import com.gentics.mesh.core.rest.node.field.impl.DateFieldImpl;
import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.util.CompareUtils;
import com.syncleus.ferma.AbstractVertexFrame;

/**
 * @see DateGraphField
 */
public class DateGraphFieldImpl extends AbstractBasicField<DateField> implements DateGraphField {

	public static FieldTransformer<DateField> DATE_TRANSFORMER = (container, ac, fieldKey, fieldSchema, languageTags, level, parentNode) -> {
		HibDateField graphDateField = container.getDate(fieldKey);
		if (graphDateField == null) {
			return null;
		} else {
			return graphDateField.transformToRest(ac);
		}
	};

	public static FieldUpdater DATE_UPDATER = (container, ac, fieldMap, fieldKey, fieldSchema, schema) -> {
		HibDateField dateGraphField = container.getDate(fieldKey);
		DateField dateField = fieldMap.getDateField(fieldKey);
		boolean isDateFieldSetToNull = fieldMap.hasField(fieldKey) && (dateField == null || dateField.getDate() == null);
		HibField.failOnDeletionOfRequiredField(dateGraphField, isDateFieldSetToNull, fieldSchema, fieldKey, schema);
		boolean restIsNullOrEmpty = dateField == null || dateField.getDate() == null;

		// Skip this check for no migrations
		if (!ac.isMigrationContext()) {
			HibField.failOnMissingRequiredField(dateGraphField, restIsNullOrEmpty, fieldSchema, fieldKey, schema);
		}

		// Handle Deletion - The field was explicitly set to null and is currently set in the graph so we can remove the field from the given container
		if (isDateFieldSetToNull && dateGraphField != null) {
			container.removeField(dateGraphField);
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

	public DateGraphFieldImpl(String fieldKey, AbstractVertexFrame parentContainer) {
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
	public void removeField(BulkActionContext bac, HibFieldContainer container) {
		setFieldProperty("date", null);
		setFieldKey(null);
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
