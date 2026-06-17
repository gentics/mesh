package com.gentics.mesh.plugin.forms.model.schema;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Strings;

import com.gentics.mesh.plugin.forms.model.Form;
import com.gentics.mesh.plugin.forms.model.FormElement;

/**
 * Implementation of a {@link LinkedHashMap} of {@link String} to {@link FormDataProperty} which holds the data POSTed, to a {@link Form}
 */
public class PostedData extends LinkedHashMap<String, FormDataProperty> {

	private static final long serialVersionUID = -6782751524948555661L;

	/**
	 * Form
	 */
	protected Form form;

	/**
	 * Create an empty instance
	 * @param form form to which the data is posted
	 */
	public PostedData(Form form) {
		super();
		this.form = form;
	}

	/**
	 * Create a prefilled instance
	 * @param form form to which the data is posted
	 * @param properties properties to prefill
	 */
	public PostedData(Form form, Map<String, FormDataProperty> properties) {
		super(properties);
		this.form = form;
	}

	/**
	 * Create an instance as copy from the given instance
	 * @param from instance to copy
	 */
	public PostedData(PostedData from) {
		super(from);
		this.form = from.form;
	}

	/**
	 * Get the value of the element with given name as string
	 * @param name element name
	 * @return string value (may be null)
	 */
	public String getString(String name) {
		Optional<FormElement> optElement = getElement(name);
		Optional<FormDataProperty> optProperty = Optional.ofNullable(get(name));

		if (optElement.isPresent() && optProperty.isPresent()) {
			FormElement element = optElement.get();
			FormDataProperty property = optProperty.get();

			if (element.isList()) {
				List<Object> values = property.getValues();
				if (CollectionUtils.isEmpty(values)) {
					return null;
				}
				return values.stream().map(element::transformPostedValue).findFirst().orElse(null);
			} else {
				return element.transformPostedValue(property.getValue());
			}
		}

		return null;
	}

	/**
	 * Get all values of the element with given name as strings
	 * @param name element name
	 * @return string values (may be empty, but not null)
	 */
	public List<String> getAllStrings(String name) {
		Optional<FormElement> optElement = getElement(name);
		Optional<FormDataProperty> optProperty = Optional.ofNullable(get(name));

		if (optElement.isPresent() && optProperty.isPresent()) {
			FormElement element = optElement.get();
			FormDataProperty property = optProperty.get();

			if (element.isList()) {
				List<Object> values = property.getValues();
				if (CollectionUtils.isEmpty(values)) {
					return List.of();
				}
				return values.stream().map(element::transformPostedValue).collect(Collectors.toList());
			} else {
				Object value = property.getValue();
				if (value != null) {
					return List.of(element.transformPostedValue(value));
				} else {
					return List.of();
				}
			}
		}
		return List.of();
	}

	/**
	 * Get the set of element names for which data are present
	 * @return set of element names
	 */
	public Set<String> names() {
		return keySet();
	}

	/**
	 * Set the given value for the element with given name
	 * @param name element name
	 * @param value value to set
	 * @return fluent API
	 */
	public PostedData set(String name, String value) {
		Optional<FormElement> optElement = getElement(name);

		if (optElement.isPresent()) {
			FormElement element = optElement.get();
			FormDataProperty property = Optional.ofNullable(get(name)).orElseGet(() -> new FormDataProperty());

			if (element.isList()) {
				property.setValues(List.of(element.transformToExternal(value)));
				property.setValue(null);
			} else {
				property.setValues(null);
				property.setValue(element.transformToExternal(value));
			}
			put(name, property);
		}
		return this;
	}

	/**
	 * Set the values for the element with given name. If the element is single-value, only the first value will be set
	 * @param name element name
	 * @param values values to set
	 * @return fluent API.
	 */
	public PostedData set(String name, Iterable<String> values) {
		Optional<FormElement> optElement = getElement(name);

		if (optElement.isPresent()) {
			FormElement element = optElement.get();
			FormDataProperty property = Optional.ofNullable(get(name)).orElseGet(() -> new FormDataProperty());

			if (element.isList()) {
				List<Object> valueList = StreamSupport.stream(values.spliterator(), false).map(element::transformToExternal)
						.collect(Collectors.toList());

				property.setValues(valueList);
				property.setValue(null);
			} else {
				property.setValues(null);
				Iterator<String> iterator = values.iterator();
				if (iterator.hasNext()) {
					property.setValue(element.transformToExternal(iterator.next()));
				} else {
					property.setValue(null);
				}
			}
			put(name, property);
		}
		return this;
	}

	/**
	 * Add the given value to the element with given name. If the element is single-value, the current value will be replaced
	 * @param name element name
	 * @param value value to add
	 * @return fluent API
	 */
	public PostedData addString(String name, String value) {
		Optional<FormElement> optElement = getElement(name);

		if (optElement.isPresent()) {
			FormElement element = optElement.get();
			FormDataProperty property = Optional.ofNullable(get(name)).orElseGet(() -> new FormDataProperty());

			if (element.isList()) {
				Optional.ofNullable(property.getValues()).orElseGet(() -> new ArrayList<>())
						.add(element.transformToExternal(value));

				property.setValue(null);
			} else {
				property.setValues(null);
				property.setValue(element.transformToExternal(value));
			}

			put(name, property);
		}
		return this;
	}

	/**
	 * Check whether the instance contains data for the element with given name
	 * @param name element name
	 * @return true, when values are present
	 */
	public boolean contains(String name) {
		return containsKey(name);
	}

	/**
	 * Get the element with given name as optional
	 * @param name name
	 * @return optional element
	 */
	protected Optional<FormElement> getElement(String name) {
		return form.getElementsInsideProperties().filter(FormElement::isActive)
				.filter(element -> Strings.CI.equals(element.getName(), name)).findFirst();
	}
}
