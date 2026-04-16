package com.gentics.mesh.plugin.forms.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.gentics.mesh.core.rest.common.FieldContainer;
import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.field.StringField;
import com.gentics.mesh.plugin.forms.i18n.I18nHelper;
import com.gentics.mesh.plugin.forms.service.FormDataService;
import io.vertx.core.json.JsonObject;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;


/**
 * Base for elements which offer a predefined selection of possible values like radio buttons or dropdown fields.
 */
public class ValueListElement extends FormElement {

	public static final String RC_VALUE_LABELS = "valueLabels";

	/**
	 * Returns a collector that maps each {@link ValueListItem} to its value (identifier) and the label
	 * for the given language. If the requested language is not available, falls back to the first
	 * available label. The resulting map preserves insertion order.
	 *
	 * @param languagelanguage tag (e.g. {@code "de"}, {@code "en"}), may be {@code null}
	 * @return collector producing a {@code LinkedHashMap<value, label>}
	 */
	public static Collector<ValueListItem, ?, Map<String, String>> optionsMapCollector(String language) {
		return Collectors.toMap(
			item -> StringUtils.defaultIfBlank(item.getValue(), ""),
			item -> {
				Map<String, String> lbl = item.getLabel();
				if (lbl == null || lbl.isEmpty()) {
					return "";
				}
				String direct = lbl.get(language);
				if (direct != null) {
					return direct;
				}
				return StringUtils.defaultIfBlank(lbl.values().iterator().next(), "");
			},
			(u, v) -> u,
			LinkedHashMap::new);
	}

	protected List<ValueListItem> selectOptions = new ArrayList<>();


	@JsonIgnore
	protected Set<String> validValues = new HashSet<>();

	public ValueListElement() {
	}

	protected ValueListElement(ValueListElement other, boolean deep) {
		super(other, deep);

		this.selectOptions = cloneField(other.selectOptions, deep);
		updateValidValues();
	}

	@JsonIgnore
	public List<ValueListItem> getSelectOptions() {
		return selectOptions;
	}

	@JsonProperty("selectOptions")
	public void setSelectOptions(List<ValueListItem> selectOptions) {
		this.selectOptions = Optional.ofNullable(selectOptions).orElse(new ArrayList<>());
	}



	/**
	 * Tests if the element has a non-empty list of elements to choose from, and all of them have a value and a label.
	 *
	 * @return The respective error, when there are no options or one of them does not have a value or a label
	 */
	@Override
	public Optional<ValidationError> validateForm() {
		ValidationError error = new ValidationError()
			.setName(getName())
			.setTitle(I18nHelper.get(Locale.GERMAN, "error.validation.form.title"));

		if (selectOptions == null || selectOptions.isEmpty()) {
			error.setMessage(I18nHelper.get(Locale.GERMAN, "error.validation.form.missing_list_elements", getLabelOrName()));

			return Optional.of(error);
		}

		Optional<ValueListItem> invalid = selectOptions.stream()
			.filter(e -> StringUtils.isEmpty(e.getValue()) || (e.getLabel() == null || e.getLabel().isEmpty()))
			.findAny();

		if (invalid.isPresent()) {
			String key = StringUtils.isEmpty(invalid.get().getValue())
				? "error.validation.form.missing_value"
				: "error.validation.form.missing_label";

			error.setMessage(I18nHelper.get(Locale.GERMAN, key, getLabelOrName()));

			return Optional.of(error);
		}

		return Optional.empty();
	}

	@Override
	public void addPollData(JsonObject results, FieldContainer formData) {
		super.addPollData(results, formData);

		results.put(
			RC_VALUE_LABELS,
			getSelectOptions().stream().sequential().collect(optionsMapCollector(null)));

		JsonObject values = results.getJsonObject(FormDataService.POLL_VALUES_NAME);

		if (values.isEmpty()) {
			// Prepare the values, to enforce element order, as they are defined
			// in the form.
			getSelectOptions().forEach(e -> values.put(e.getValue(), 0));
		}

		String fieldName = getName();
		FieldMap fields = formData.getFields();

		if (isList()) {
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

	@Override
	protected FormElement clone(boolean deep) {
		return new ValueListElement(this, deep);
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

	@Override
	protected boolean isInvalidValue(String value) {
		// This is also checked in validateForm(), but just to be sure.
		if (CollectionUtils.isEmpty(selectOptions)) {
			return true;
		}

		return !validValues.contains(value);
	}

	/**
	 * Prepare the set of valid values from the current collection of options.
	 */
	private void updateValidValues() {
		validValues.clear();
		validValues.addAll(selectOptions.stream().map(ValueListItem::getValue).collect(Collectors.toSet()));
	}
}
