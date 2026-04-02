package com.gentics.mesh.hibernate.data.node.field.impl;

import com.gentics.mesh.core.data.node.field.HibJsonField;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.hibernate.data.domain.HibUnmanagedFieldContainer;

import io.vertx.core.json.JsonObject;

/**
 * JSON object field of Hibernate content.
 * 
 * @author plyhun
 *
 */
public class HibJsonFieldImpl extends AbstractBasicHibField<JsonObject> implements HibJsonField {

	public HibJsonFieldImpl(String fieldKey, HibUnmanagedFieldContainer<?, ?, ?, ?, ?> parent, JsonObject value) {
		super(fieldKey, parent, FieldTypes.JSON, value);
	}

	@Override
	public JsonObject getJson() {
		return valueOrNull();
	}

	@Override
	public void setJson(JsonObject string) {
		storeValue(string);
	}

	@Override
	public boolean equals(Object obj) {
		return jsonEquals(obj);
	}
}
