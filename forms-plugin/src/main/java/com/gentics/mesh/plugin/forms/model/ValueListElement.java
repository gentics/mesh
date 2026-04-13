package com.gentics.mesh.plugin.forms.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.gentics.mesh.core.rest.common.FieldContainer;
import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.field.StringField;
import com.gentics.mesh.plugin.forms.i18n.I18nHelper;
import com.gentics.mesh.plugin.forms.service.FormDataService;
import io.vertx.core.json.JsonObject;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import javax.annotation.Nonnull;
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

//TODO 8.5 delete ? 

/**
 * Base for elements which offer a predefined selection of possible values like radio buttons or dropdown fields.
 */
public class ValueListElement extends FormElement {

	public static final String RC_VALUE_LABELS = "valueLabels";

	/** Collect a stream of {@link ValueListItem} into a linked hash map preserving the order of the items. */
	public static final Collector<ValueListItem, ?, Map<String, String>> OPTIONS_MAP_COLLECTOR = Collectors.toMap(
		item -> StringUtils.defaultIfBlank(item.getKey(), ""),
		item -> StringUtils.defaultIfBlank(item.getValue(), ""),
		(u, v) -> u,
		LinkedHashMap::new);

	@JsonIgnore
	protected List<ValueListItem> options = new ArrayList<>();

	@JsonIgnore
	protected Set<String> validValues = new HashSet<>();

	public ValueListElement() {
	}

	protected ValueListElement(ValueListElement other, boolean deep) {
		super(other, deep);

		this.options = cloneField(other.options, deep);
		updateValidValues();
	}

	/**
	 * Get the options of this value list element.
	 * @return The options of this value list element
	 */
	@Nonnull
	public List<ValueListItem> getOptions() {
		if (options == null) {
			options = new ArrayList<>();
			updateValidValues();
		}

		return options;
	}

	/**
	 * Set the options for this value list element.
	 * @param options The options for this value list element
	 */
	public void setOptions(List<ValueListItem> options) {
		this.options = Optional.ofNullable(options).orElse(new ArrayList<>());
		updateValidValues();
	}

	@JsonProperty
	@JsonIgnore
	@Deprecated
	public List<LegacyValueListItem> getListelements() {
		return getOptions().stream()
			.map(i -> new LegacyValueListItem().setValue(i.getKey()).setLabel(i.getValue()))
			.collect(Collectors.toList());
	}

	@JsonProperty
	@Deprecated
	public void setListelements(List<LegacyValueListItem> listelements) {
		if (listelements == null) {
			this.options = new ArrayList<>();
			updateValidValues();

			return;
		}

		this.options = listelements.stream()
			.map(i -> new ValueListItem().setKey(i.getValue()).setValue(i.getLabel()))
			.collect(Collectors.toList());

		updateValidValues();
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

		if (options == null || options.isEmpty()) {
			error.setMessage(I18nHelper.get(Locale.GERMAN, "error.validation.form.missing_list_elements", getLabelOrName()));

			return Optional.of(error);
		}

		Optional<ValueListItem> invalid = options.stream()
			.filter(e -> StringUtils.isEmpty(e.getKey()) || StringUtils.isEmpty(e.getValue()))
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
			getOptions().stream().sequential().collect(OPTIONS_MAP_COLLECTOR));

		JsonObject values = results.getJsonObject(FormDataService.POLL_VALUES_NAME);

		if (values.isEmpty()) {
			// Prepare the values, to enforce element order, as they are defined
			// in the form.
			getOptions().forEach(e -> values.put(e.getKey(), 0));
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
		if (CollectionUtils.isEmpty(options)) {
			return true;
		}

		return !validValues.contains(value);
	}

	/**
	 * Prepare the set of valid values from the current collection of options.
	 */
	private void updateValidValues() {
		validValues.clear();
		validValues.addAll(options.stream().map(ValueListItem::getKey).collect(Collectors.toSet()));
	}
}
