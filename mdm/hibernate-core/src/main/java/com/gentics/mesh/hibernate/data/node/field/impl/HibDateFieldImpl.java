package com.gentics.mesh.hibernate.data.node.field.impl;

import java.time.Instant;

import com.gentics.mesh.core.data.node.field.DateField;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.hibernate.data.domain.HibUnmanagedFieldContainer;

/**
 * Date field of Hibernate content.
 * 
 * @author plyhun
 *
 */
public class HibDateFieldImpl extends AbstractBasicHibField<Instant> implements DateField {

	public HibDateFieldImpl(String fieldKey, HibUnmanagedFieldContainer<?, ?, ?, ?, ?> parent, Instant value) {
		super(fieldKey, parent, FieldTypes.DATE, value);
	}

	@Override
	public void setDate(Long date) {
		Instant value = Instant.ofEpochMilli(date);
		storeValue(value);
	}

	@Override
	public Long getDate() {
		return value.map(Instant::toEpochMilli).orElse(null);
	}

	@Override
	public boolean equals(Object obj) {
		return dateEquals(obj);
	}
}
