package com.gentics.mesh.hibernate.data.node.field.impl;

import com.gentics.mesh.core.data.node.field.NumberField;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.hibernate.data.domain.HibUnmanagedFieldContainer;

/**
 * Number field of Hibernate content.
 * 
 * @author plyhun
 *
 */
public class HibNumberFieldImpl extends AbstractBasicHibField<Number> implements NumberField {
	/**
	 * Convert to given value to an instance of Long, if that is possible without losing precision
	 * @param value given value
	 * @return either the value as Long or unmodified
	 */
	public static Number convertToLongIfPossible(Number value) {
		if (value != null && (value.doubleValue() % 1) == 0 && value.doubleValue() <= Long.MAX_VALUE) {
			return Long.valueOf(value.longValue());
		}
		return value;
	}

	/**
	 * Convert the value to a double
	 * @param value given value
	 * @return value as double (or null if value was null)
	 */
	public static Double convertToDouble(Number value) {
		if (value != null) {
			return Double.valueOf(value.doubleValue());
		} else {
			return null;
		}
	}

	public HibNumberFieldImpl(String fieldKey, HibUnmanagedFieldContainer<?, ?, ?, ?, ?> parent, Number value) {
		super(fieldKey, parent, FieldTypes.NUMBER, convertToDouble(value));
	}

	@Override
	public void setNumber(Number number) {
		number = convertToDouble(number);
		storeValue(number);
	}

	@Override
	public Number getNumber() {
		return convertToLongIfPossible(valueOrNull());
	}

	@Override
	public boolean equals(Object obj) {
		return numberEquals(obj);
	}
}
