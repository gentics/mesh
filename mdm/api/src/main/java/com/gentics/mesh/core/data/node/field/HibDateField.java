package com.gentics.mesh.core.data.node.field;

import static com.gentics.mesh.util.DateUtils.toISO8601;

import com.gentics.mesh.core.data.HibField;
import com.gentics.mesh.core.data.HibFieldContainer;
import com.gentics.mesh.core.data.node.field.nesting.HibListableField;
import com.gentics.mesh.core.rest.node.field.DateField;
import com.gentics.mesh.core.rest.node.field.impl.DateFieldImpl;
import com.gentics.mesh.handler.ActionContext;

public interface HibDateField extends HibListableField, HibBasicField<DateField> {

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
	default HibField cloneTo(HibFieldContainer container) {
		HibDateField clone = container.createDate(getFieldKey());
		clone.setDate(getDate());
		return clone;
	}

	@Override
	default DateField transformToRest(ActionContext ac) {
		DateField dateField = new DateFieldImpl();
		dateField.setDate(toISO8601(getDate()));
		return dateField;
	}
}
