package com.gentics.mesh.core.rest.node.response.field;

import org.codehaus.jackson.annotate.JsonProperty;

import com.gentics.mesh.model.FieldTypes;

public class ListField extends AbstractField {

	private String listType;
	private Integer min;
	private Integer max;

	@JsonProperty("allowed")
	private String[] allowedSchemas;

	public String[] getAllowedSchemas() {
		return allowedSchemas;
	}

	public void setAllowedSchemas(String[] allowedSchemas) {
		this.allowedSchemas = allowedSchemas;
	}

	public String getListType() {
		return listType;
	}

	public void setListType(String listType) {
		this.listType = listType;
	}

	public Integer getMax() {
		return max;
	}

	public void setMax(Integer max) {
		this.max = max;
	}

	public Integer getMin() {
		return min;
	}

	public void setMin(Integer min) {
		this.min = min;
	}

	@Override
	public String getType() {
		return FieldTypes.LIST.toString();
	}
}
