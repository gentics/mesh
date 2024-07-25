package com.gentics.mesh.plugin.forms.model;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gentics.mesh.core.rest.common.FieldContainerModel;

import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Element for select groups
 */
@Reportable
public class SelectgroupElement extends CombiElement {

	@JsonProperty("additional_element")
	private boolean additionalElement;

	public SelectgroupElement() {
	}

	protected SelectgroupElement(SelectgroupElement other, boolean deep) {
		super(other, deep);

		this.additionalElement = other.additionalElement;
	}

	/**
	 * Get the "additional_element" flag
	 * @return true for additional element
	 */
	@JsonProperty("additional_element")
	public boolean isAdditionalElement() {
		return additionalElement;
	}

	/**
	 * Set the "additional_element" flag
	 * @param additionalElement flag
	 */
	@JsonProperty("additional_element")
	public void setAdditionalElement(boolean additionalElement) {
		this.additionalElement = additionalElement;
	}

	@Override
	public String getSingleFormData(MultiMap postedData) {
		return transform(super.getSingleFormData(postedData), postedData);
	}

	@Override
	public List<String> getMultiFormData(MultiMap postedData) {
		List<String> values = super.getMultiFormData(postedData);
		if (values == null) {
			return null;
		} else {
			return values.stream().map(value -> transform(value, postedData)).filter(value -> value != null).collect(Collectors.toList());
		}
	}

	/**
	 * Add the element data and additional element data stored in the container to
	 * the json object
	 * 
	 * @param object    object (will be modified)
	 * @param container field container
	 * @param key       key function
	 */
	public void addFormData(JsonObject object, FieldContainerModel container, Function<FormElement, String> key) {
		super.addFormData(object, container, key);

		List<String> options = getOptions().stream().map(ValueListItem::getKey).collect(Collectors.toList());

		// check if posted data contains the value of an additional element
		if (isMultivalue()) {
			JsonArray postedData = object.getJsonArray(key.apply(this));
			if (postedData != null) {
				for (Object postedValue : postedData) {
					if (!options.contains(postedValue)) {
						// posted value is not part of regular options thus it must belong to an
						// additional element
						object.put("additional_element", postedValue);
					}
				}
			}
		} else {
			String submittedValue = object.getString(key.apply(this));
			if (submittedValue != null && !options.contains(submittedValue)) {
				// submitted value is not part of regular options thus it must belong to an
				// additional element
				object.put("additional_element", submittedValue);
			}
		}
	}

	/**
	 * Transform the value:
	 * <ol>
	 * <li>If the element has an additional element, and the posted value is "[name]additional_field", get the data posted with this name instead</li>
	 * <li>Return the posted value in other cases</li>
	 * </ol>
	 * @param value
	 * @return
	 */
	protected String transform(String value, MultiMap postedData) {
		if (isAdditionalElement()) {
			if (String.format("%sadditional_field", getName()).equals(value)) {
				
				return postedData.get(String.format("%s_additional", getName()));
			}
		}
		return value;
	}

	@Override
	public boolean isMultivalue() {
		return (getSelectType() != null && getSelectType().startsWith("multiselect"));
	}

	@Override
	public FormElement clone(boolean deep) {
		return new SelectgroupElement(this, deep);
	}
}
