package com.gentics.mesh.core.data.node.field;

import static com.gentics.mesh.util.DateUtils.fromISO8601;
import static com.gentics.mesh.util.DateUtils.toISO8601;

import com.gentics.mesh.core.data.Field;
import com.gentics.mesh.core.data.FieldContainer;
import com.gentics.mesh.core.data.node.field.nesting.ListableField;
import com.gentics.mesh.core.rest.node.field.DateFieldModel;
import com.gentics.mesh.core.rest.node.field.impl.DateFieldImpl;
import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.util.CompareUtils;

public interface DateField extends ListableField, BasicField<DateFieldModel> {

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

	@Override
	default Field cloneTo(FieldContainer container) {
		DateField clone = container.createDate(getFieldKey());
		clone.setDate(getDate());
		return clone;
	}

	@Override
	default DateFieldModel transformToRest(ActionContext ac) {
		DateFieldModel dateField = new DateFieldImpl();
		dateField.setDate(toISO8601(getDate()));
		return dateField;
	}

	default boolean dateEquals(Object obj) {
		if (obj instanceof DateField) {
			Long dateA = getDate();
			Long dateB = ((DateField) obj).getDate();
			return CompareUtils.equals(dateA, dateB);
		}
		if (obj instanceof DateFieldModel) {
			Long dateA = getDate();
			Long dateB = fromISO8601(((DateFieldModel) obj).getDate());
			return CompareUtils.equals(dateA, dateB);
		}
		return false;
	}
}
