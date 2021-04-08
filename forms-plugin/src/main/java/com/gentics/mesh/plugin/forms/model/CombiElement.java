package com.gentics.mesh.plugin.forms.model;

import com.fasterxml.jackson.annotation.JsonProperty;

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
}
