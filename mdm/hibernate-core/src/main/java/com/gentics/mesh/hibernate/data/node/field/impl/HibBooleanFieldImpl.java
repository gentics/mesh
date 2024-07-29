package com.gentics.mesh.hibernate.data.node.field.impl;

import com.gentics.mesh.core.data.node.field.HibBooleanField;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.hibernate.data.domain.HibUnmanagedFieldContainer;

/**
 * Boolean field of Hibernate content.
 * 
 * @author plyhun
 *
 */
public class HibBooleanFieldImpl extends AbstractBasicHibField<Boolean> implements HibBooleanField {

	public HibBooleanFieldImpl(String fieldKey, HibUnmanagedFieldContainer<?, ?, ?, ?, ?> parent, Boolean value) {
		super(fieldKey, parent, FieldTypes.BOOLEAN, value);
	}

	@Override
	public Boolean getBoolean() {
		return valueOrNull();
	}

	@Override
	public void setBoolean(Boolean bool) {
		storeValue(bool);
	}

	@Override
	public boolean equals(Object obj) {
		return booleanEquals(obj);		
	}
}
