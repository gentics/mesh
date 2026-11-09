package com.gentics.mesh.plugin.forms.model;

import java.util.Optional;

import com.gentics.mesh.plugin.forms.context.FormDataCreateContext;
import com.gentics.mesh.plugin.forms.model.schema.FormUiSchema;
import com.gentics.mesh.plugin.forms.model.schema.UischemaFormgridOptions;
import io.reactivex.Maybe;
import io.vertx.reactivex.core.Vertx;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gentics.mesh.core.rest.common.FieldContainer;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.plugin.forms.service.FormDataService;

import io.vertx.core.json.JsonObject;

/**
 * Checkbox field
 */
@Reportable
@FormElementType("boolean")
public class BooleanElement extends FormElement {

	@JsonProperty
	private String value;

	public BooleanElement() {
	}

	protected BooleanElement(BooleanElement other, boolean deep) {
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
	public void addPollData(JsonObject results, FieldContainer formData, String language, FormUiSchema uiSchema) {
		super.addPollData(results, formData, language, uiSchema);

		JsonObject values = results.getJsonObject(FormDataService.POLL_VALUES_NAME);

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
		return new BooleanElement(this, deep);
	}

	@Override
	public Maybe<ValidationError> validate(FormDataCreateContext ctx, Vertx rxVertx, OkHttpClient httpClient, FormUiSchema uischema) {
		// When the value of the element is not directly set, check if the "storedValue" property is set in the
		// UI schemas formgrid options.
		if (this.value == null) {
			this.value = getFormGridOptionsFromUiSchema(uischema)
				.map(UischemaFormgridOptions::getAny)
				.map(props -> props.get("storedValue"))
				.map(Object::toString)
				.orElse(null);
		}

		return super.validate(ctx, rxVertx, httpClient, uischema);
	}

	@Override
	protected boolean isInvalidValue(String value) {
		var actualValue = Optional.ofNullable(value).map(String::trim).orElse(null);

		return StringUtils.isNotBlank(this.value) && !this.value.trim().equals(actualValue);
	}
}
