package com.gentics.mesh.hibernate.data.node.field.impl;

import com.gentics.mesh.core.data.node.field.HibJsonField;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.node.field.JsonContent;
import com.gentics.mesh.hibernate.data.domain.HibUnmanagedFieldContainer;

/**
 * JSON object field of Hibernate content.
 * 
 * @author plyhun
 *
 */
public class HibJsonFieldImpl extends AbstractBasicHibField<JsonContent> implements HibJsonField {

	public HibJsonFieldImpl(String fieldKey, HibUnmanagedFieldContainer<?, ?, ?, ?, ?> parent, JsonContent value) {
		super(fieldKey, parent, FieldTypes.JSON, value);
	}

	@Override
	public JsonContent getJson() {
		return valueOrNull();
	}

	@Override
	public void setJson(JsonContent string) {
		storeValue(string);
	}

	@Override
	public boolean equals(Object obj) {
		return jsonEquals(obj);
	}
}
