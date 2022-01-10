package com.gentics.mesh.plugin.forms.model;

import com.gentics.mesh.core.rest.common.FieldContainer;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.StringUtils;

/**
 * Checkbox field
 */
@Reportable
public class CheckboxElement extends FormElement {

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
}
