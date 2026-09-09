package com.gentics.mesh.core.data.node.field;

import com.fasterxml.jackson.databind.JsonNode;
import com.gentics.mesh.core.data.HibField;
import com.gentics.mesh.core.data.HibFieldContainer;
import com.gentics.mesh.core.data.node.field.nesting.HibListableField;
import com.gentics.mesh.core.rest.node.field.JsonField;
import com.gentics.mesh.core.rest.node.field.impl.JsonFieldImpl;
import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.util.CompareUtils;

public interface HibJsonField extends HibListableField, HibBasicField<JsonField> {

	/**
	 * Set the JSON object within the field.
	 * 
	 * @param json
	 */
	void setJson(JsonNode json);

	/**
	 * Return the JSON object which is stored in the field.
	 * 
	 * @return
	 */
	JsonNode getJson();

	@Override
	default HibField cloneTo(HibFieldContainer container) {
		HibJsonField clone = container.createJson(getFieldKey());
		clone.setJson(getJson());
		return clone;
	}

	@Override
	default JsonField transformToRest(ActionContext ac) {
		JsonField jsonField = new JsonFieldImpl();
		jsonField.setJson(getJson());
		return jsonField;
	}

	default boolean jsonEquals(Object obj) {
		if (obj instanceof HibJsonField otherField) {
			JsonNode jsonA = getJson();
			JsonNode jsonB = otherField.getJson();
			return CompareUtils.equals(jsonA, jsonB);
		}
		if (obj instanceof JsonField otherField) {
			JsonNode jsonA = getJson();
			JsonNode jsonB = otherField.getJson();
			return CompareUtils.equals(jsonA, jsonB);
		}
		return false;
	}
}
