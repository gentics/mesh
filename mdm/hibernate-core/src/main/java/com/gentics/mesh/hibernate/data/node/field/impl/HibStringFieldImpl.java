package com.gentics.mesh.hibernate.data.node.field.impl;

import com.gentics.mesh.core.data.node.field.HibStringField;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.hibernate.data.domain.HibUnmanagedFieldContainer;

/**
 * String field of Hibernate content.
 * 
 * @author plyhun
 *
 */
public class HibStringFieldImpl extends AbstractBasicHibField<String> implements HibStringField {

	public HibStringFieldImpl(String fieldKey, HibUnmanagedFieldContainer<?, ?, ?, ?, ?> parent, String value) {
		super(fieldKey, parent, FieldTypes.STRING, value);
	}

	@Override
	public String getString() {
		return valueOrNull();
	}

	@Override
	public void setString(String string) {
		storeValue(string);
	}

	@Override
	public boolean equals(Object obj) {
		return stringEquals(obj);
	}
}
