package com.gentics.mesh.plugin.forms.model;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.vertx.core.MultiMap;
import org.apache.commons.lang3.tuple.Pair;

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
		return Objects.equals("multiselect", getSelectType());
	}

	@Override
	public FormElement clone(boolean deep) {
		return new SelectgroupElement(this, deep);
	}
}
