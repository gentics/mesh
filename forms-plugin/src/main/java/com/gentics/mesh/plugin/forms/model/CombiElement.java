package com.gentics.mesh.plugin.forms.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gentics.mesh.core.rest.common.FieldContainer;
import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.field.StringField;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.Optional;

/**
 * Combi Element
 */
@Reportable
public class CombiElement extends ValueListElement {
	@JsonProperty("select_type")
	private String selectType;

	/**
	 * Get the select_type
	 * @return select_type
	 */
	@JsonProperty("select_type")
	public String getSelectType() {
		return selectType;
	}

	@JsonProperty("select_type")
	public void setSelectType(String selectType) {
		this.selectType = selectType;
	}

	@Override
	public boolean isMultivalue() {
		return "Checkbox".equals(getSelectType());
	}

	@Override
	public void addPollData(JsonObject results, FieldContainer formData) {
		super.addPollData(results, formData);

		results.put("select_type", selectType);

		JsonObject values = results.getJsonObject("values");
		String fieldName = getName();
		FieldMap fields = formData.getFields();

		if (isMultivalue()) {
			Optional<List<String>> allValues = Optional.ofNullable(fields)
				.map(f -> f.getStringFieldList(fieldName))
				.map(l -> l.getItems());

			if (allValues.isPresent()) {
				allValues.get().forEach(value -> inc(values, value));
			}
		} else {
			Optional<String> value = Optional.ofNullable(fields)
				.map(f -> f.getStringField(fieldName))
				.map(StringField::getString);

			if (value.isPresent()) {
				inc(values, value.get());
			}
		}
	}

	/**
	 * Increment the integer field with the name {@code value} in the given
	 * JSON object {@code results} by one, or set the field to 0 if it does not
	 * already exist.
	 *
	 * @param results The JSON Object to update
	 * @param value The name of the integer field to increment
	 */
	private void inc(JsonObject results, String value) {
		if (value == null) {
			return;
		}

		int count = results.getInteger(value, 0) + 1;

		results.put(value, count);
	}
}
