package com.gentics.mesh.hibernate.data.node.field.impl;

import java.util.Optional;

import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.hibernate.data.domain.HibUnmanagedFieldContainer;
import com.gentics.mesh.util.CompareUtils;

/**
 * The extension to {@link AbstractHibField} to store the field value of a selected type.
 * 
 * @author plyhun
 *
 * @param <T> the value type
 */
public abstract class AbstractBasicHibField<T> extends AbstractHibField {

	protected final FieldTypes fieldTypes;
	protected Optional<T> value;

	public AbstractBasicHibField(String fieldKey, HibUnmanagedFieldContainer<?, ?, ?, ?, ?> parent, FieldTypes fieldTypes, T value) {
		super(fieldKey, parent);
		this.fieldTypes = fieldTypes;
		this.value = Optional.ofNullable(value);
	}

	@Override
	public void validate() {
	}

	/**
	 * Store the value in the field itself and the container, the field is attached to.
	 * 
	 * @param value
	 */
	public void storeValue(T value) {
		this.getContainer().storeValue(this, value);
		this.value = Optional.ofNullable(value);
	}

	/**
	 * Get the value from the field. May give back null.
	 * 
	 * @return
	 */
	public T valueOrNull() {
		return value.orElse(null);
	}

	public FieldTypes getFieldTypes() {
		return fieldTypes;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean equals(Object obj) {
		if (getClass().isInstance(obj)) {
			Optional<T> valueA = this.value;
			Optional<T> valueB = ((AbstractBasicHibField<T>) obj).value;
			return CompareUtils.equals(valueA, valueB);
		}
		return false;
	}

	/**
	 * Get the value type of this field.
	 * 
	 * @return
	 */
	public FieldTypes getFieldType() {
		return fieldTypes;
	}

	/**
	 * The field constructor reference, used to build the field with the value, read from the storage.
	 * 
	 * @author plyhun
	 *
	 * @param <T> the value type
	 * @param <F> the field type
	 */
	@FunctionalInterface
	public static interface BasicHibFieldConstructor<T, F extends AbstractBasicHibField<T>> {
		F create(String fieldKey, HibUnmanagedFieldContainer<?,?,?,?,?> parent, T value);
	}
}
