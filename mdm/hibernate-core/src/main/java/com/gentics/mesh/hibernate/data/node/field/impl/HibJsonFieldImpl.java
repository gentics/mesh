package com.gentics.mesh.hibernate.data.node.field.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.gentics.mesh.core.data.node.field.HibJsonField;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.hibernate.data.domain.HibUnmanagedFieldContainer;

/**
 * JSON object field of Hibernate content.
 * 
 * @author plyhun
 *
 */
public class HibJsonFieldImpl extends AbstractBasicHibField<JsonNode> implements HibJsonField {

	public HibJsonFieldImpl(String fieldKey, HibUnmanagedFieldContainer<?, ?, ?, ?, ?> parent, JsonNode value) {
		super(fieldKey, parent, FieldTypes.JSON, value);
	}

	@Override
	public JsonNode getJson() {
		return valueOrNull();
	}

	@Override
	public void setJson(JsonNode string) {
		storeValue(string);
	}

	@Override
	public boolean equals(Object obj) {
		return jsonEquals(obj);
	}
}
