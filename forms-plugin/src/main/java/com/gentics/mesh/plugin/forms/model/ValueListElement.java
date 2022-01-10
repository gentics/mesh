package com.gentics.mesh.plugin.forms.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gentics.mesh.core.rest.common.FieldContainer;
import com.gentics.mesh.plugin.forms.i18n.I18nHelper;

import io.vertx.core.json.JsonObject;
import org.apache.commons.lang.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Base for elements which offer a predefined selection of possible values like radio buttons or dropdown fields.
 */
public class ValueListElement extends FormElement {

	@JsonProperty("listelements")
	private List<ValueListItem> listElements;

	public List<ValueListItem> getListElements() {
		return listElements;
	}

	public void setListElements(List<ValueListItem> listElements) {
		this.listElements = listElements;
	}

	/**
	 * Tests if the element has a non-empty list of elements to choose from, and all of them have a value and a label.
	 *
	 * @return The respective error, when there are no listelements or one of them does not have a value or a label
	 */
	@Override
	public Optional<ValidationError> validateForm() {
		ValidationError error = new ValidationError()
			.setName(getName())
			.setTitle(I18nHelper.get(Locale.GERMAN, "error.validation.form.title"));

		if (listElements == null || listElements.isEmpty()) {
			error.setMessage(I18nHelper.get(Locale.GERMAN, "error.validation.form.missing_list_elements", getLabel()));

			return Optional.of(error);
		}

		Optional<ValueListItem> invalid = listElements.stream()
			.filter(e -> StringUtils.isEmpty(e.getLabel()) || StringUtils.isEmpty(e.getValue()))
			.findAny();

		if (invalid.isPresent()) {
			String key = StringUtils.isEmpty(invalid.get().getValue())
				? "error.validation.form.missing_value"
				: "error.validation.form.missing_label";

			error.setMessage(I18nHelper.get(Locale.GERMAN, key, getLabel()));

			return Optional.of(error);
		}

		return Optional.empty();
	}

	@Override
	public void addPollData(JsonObject results, FieldContainer formData) {
		super.addPollData(results, formData);

		JsonObject values = results.getJsonObject("values");

		if (values.isEmpty() && listElements != null) {
			// Prepare the values, to enforce element order, as they are defined
			// in the form.
			listElements.forEach(e -> values.put(e.getValue(), 0));
		}
	}
}
