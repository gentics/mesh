package com.gentics.mesh.plugin.forms.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gentics.mesh.core.rest.common.FieldContainer;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.StringUtils;

/**
 * Checkbox field
 */
@Reportable
public class CheckboxElement extends FormElement {

	@JsonProperty
	private String value;

	public CheckboxElement() {
	}

	protected CheckboxElement(CheckboxElement other, boolean deep) {
		super(other, deep);

		this.value = other.value;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	@Override
	public void addPollData(JsonObject results, FieldContainer formData) {
		super.addPollData(results, formData);

		JsonObject values = results.getJsonObject("values");

		if (!values.containsKey("yes")) {
			values.put("yes", 0L);
		}

		if (!values.containsKey("no")) {
			values.put("no", 0L);
		}

		StringFieldImpl field = formData.getFields().getStringField(getName());

		String key = field != null  && StringUtils.isNotBlank(field.getString()) ? "yes" : "no";
		long val = values.getLong(key) + 1;

		values.put(key, val);
	}

	@Override
	protected FormElement clone(boolean deep) {
		return new CheckboxElement(this, deep);
	}
}
